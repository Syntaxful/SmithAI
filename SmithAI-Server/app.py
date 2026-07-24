# SmithAI-Server
# External AI brain for the SmithAI Minecraft plugin.
# Supports Smith-Mini (rule-based, no model), SmithGPT 1.0 (2.2GB), and SmithGPT 2.0 (4.4GB).
#
# Environment variables:
#   SMITHAI_API_TOKEN           optional. Fixed API key for the Minecraft plugin. If unset,
#                               a key beginning with "SMA-" is generated on first boot and
#                               stored in config.yml so the plugin can be configured with
#                               /SmithAPI set SMA-...
#   SMITHAI_MODEL_TOKEN         optional. Forwarded to llama-cpp-python for HF-licensed
#                               model downloads. The server does not require it at runtime.
#   PORT                        optional. Override the listen port (default 8000).
#   SMITHAI_CONFIG              optional. Path to a YAML config file (default ./config.yml).
#
# Endpoints:
#   GET  /             liveness, model info
#   GET  /health       structured health response
#   GET  /skills       skill list for the active tier
#   POST /chat         multi-turn conversation. Body: {messages, knowledge, skills, task,
#                       context} where context carries inventory and player state.
#   POST /task         plan a multi-step task into a skill plan
#   POST /feedback     record user feedback (mirrors the Minecraft plugin)
#   POST /v1/chat/completions   OpenAI-compatible shim used by the plugin's external AI

import asyncio
import os
import random
import re
import secrets
import string
import time
from contextlib import asynccontextmanager
from typing import Any, Dict, List, Optional, Tuple

import uvicorn
import yaml
from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel

# llama-cpp-python is optional; server keeps running without it (rule-based only).
try:
    from llama_cpp import Llama
    LLAMA_AVAILABLE = True
except ImportError:
    Llama = None
    LLAMA_AVAILABLE = False


# ---------------------------------------------------------------------------
# Config & globals
# ---------------------------------------------------------------------------

CONFIG_PATH = os.environ.get("SMITHAI_CONFIG", "config.yml")
with open(CONFIG_PATH, "r") as f:
    config: Dict[str, Any] = yaml.safe_load(f) or {}

MODEL_PATH = config.get("model", {}).get("path", "models/smithgpt-1.0-2.2gb.gguf")
MODEL_NAME = config.get("model", {}).get("name", "SmithGPT 1.0 2.2GB")
MODEL_TIER = config.get("model", {}).get("tier", _infer_tier(MODEL_NAME) if (lambda: True) else "gpt1")
HOST = config.get("server", {}).get("host", "0.0.0.0")
PORT = int(os.environ.get("PORT", config.get("server", {}).get("port", 8000)))
CONTEXT_SIZE = config.get("model", {}).get("context_size", 4096)
MAX_TOKENS = config.get("model", {}).get("max_tokens", 220)
N_THREADS = config.get("model", {}).get("n_threads", 2)
API_KEY = config.get("security", {}).get("api_key", "") or os.environ.get("SMITHAI_API_TOKEN", "")

security = HTTPBearer(auto_error=False)
llm = None
authenticated = False

# Server-side conversation cache: conversation_id -> {messages, last_used, summary}.
# The plugin passes a stable conversation id (NPC id) so memory persists across calls
# even when the Minecraft client closes and re-opens.
CONV_CACHE: Dict[str, Dict[str, Any]] = {}
CONV_CACHE_TTL = 60 * 60  # 1 hour after last use
CONV_CACHE_LIMIT = 256

# Rate limit so a runaway client cannot drain the local model.
RATE_WINDOW = 30.0
RATE_MAX = 8
_rate_buckets: Dict[str, List[float]] = {}


def _infer_tier(name_or_path: str) -> str:
    s = name_or_path.lower()
    if "2.0" in s or "4.4" in s or "gpt2" in s:
        return "gpt2"
    if "1.0" in s or "2.2" in s or "gpt1" in s:
        return "gpt1"
    return "mini"


def _tier() -> str:
    if MODEL_TIER in ("gpt1", "gpt2", "mini"):
        return MODEL_TIER
    return _infer_tier(MODEL_NAME)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _generate_api_key() -> str:
    suffix = "".join(secrets.choice(string.ascii_letters + string.digits) for _ in range(32))
    return f"SMA-{suffix}"


def _save_config() -> None:
    with open(CONFIG_PATH, "w") as f:
        yaml.safe_dump(config, f, default_flow_style=False)


def _get_or_create_api_key() -> str:
    global API_KEY
    if not API_KEY:
        API_KEY = _generate_api_key()
        config.setdefault("security", {})["api_key"] = API_KEY
        _save_config()
    return API_KEY


def _print_banner(spam_network: bool = False) -> None:
    print("\n" + "=" * 64)
    print("  SMITHAI SERVER v2.1.0")
    print(f"  Model tier : {_tier()}")
    print(f"  Model name : {MODEL_NAME}")
    if LLAMA_AVAILABLE and os.path.exists(MODEL_PATH):
        print(f"  Model path : {MODEL_PATH} (loaded)")
    else:
        print(f"  Model path : {MODEL_PATH} (rule-based fallback)")
    print(f"  Listen     : {HOST}:{PORT}")
    if spam_network:
        print(f"  API key    : {_get_or_create_api_key()}")
        print("  Paste into Minecraft: /SmithAPI set " + _get_or_create_api_key())
    print("=" * 64 + "\n")


def _purge_stale_cache() -> None:
    now = time.time()
    stale = [k for k, v in CONV_CACHE.items() if now - v.get("last_used", now) > CONV_CACHE_TTL]
    for k in stale:
        CONV_CACHE.pop(k, None)
    if len(CONV_CACHE) > CONV_CACHE_LIMIT:
        # Drop oldest conversations first.
        ordered = sorted(CONV_CACHE.items(), key=lambda kv: kv[1].get("last_used", now))
        for k, _ in ordered[: len(CONV_CACHE) - CONV_CACHE_LIMIT]:
            CONV_CACHE.pop(k, None)


def _touch_cache(conversation_id: str, messages: List[Dict[str, str]]) -> None:
    if not conversation_id:
        return
    entry = CONV_CACHE.setdefault(conversation_id, {"messages": [], "summary": ""})
    entry["messages"] = messages[-30:]  # cap at 30 entries per conversation
    entry["last_used"] = time.time()
    _purge_stale_cache()


# ---------------------------------------------------------------------------
# Authentication
# ---------------------------------------------------------------------------

async def _verify_token(creds: Optional[HTTPAuthorizationCredentials] = Depends(security)) -> str:
    global authenticated
    expected = _get_or_create_api_key()
    supplied = creds.credentials if creds else ""
    if supplied != expected:
        raise HTTPException(status_code=401, detail="Invalid or missing SmithAI API key")
    if not authenticated:
        authenticated = True
        print("\n[SmithAI] Minecraft plugin authenticated.")
    return supplied


def _check_rate(client_id: str) -> None:
    now = time.time()
    bucket = _rate_buckets.setdefault(client_id, [])
    bucket[:] = [t for t in bucket if now - t < RATE_WINDOW]
    if len(bucket) >= RATE_MAX:
        raise HTTPException(status_code=429, detail="SmithAI rate limit exceeded; slow down.")
    bucket.append(now)


# ---------------------------------------------------------------------------
# Version & inventory helpers
# ---------------------------------------------------------------------------

class VersionContext:
    def __init__(self, context: Dict[str, Any]) -> None:
        self.context = context or {}
        self.version = str(self.context.get("minecraft_version", "")).strip()
        self.server_type = str(self.context.get("server_type", "java")).lower()
        self.has_deepslate = bool(self.context.get("has_deepslate", True))
        self.has_netherite = bool(self.context.get("has_netherite", True))
        self.diamond_y = self.context.get("diamond_y", -59)
        self.iron_y = self.context.get("iron_y", 16)
        self.gold_y = self.context.get("gold_y", -16)

    def is_legacy(self) -> bool:
        try:
            parts = self.version.split(".")
            minor = int(parts[1]) if len(parts) > 1 else 8
            return minor < 17
        except Exception:
            return False

    def is_eaglercraft(self) -> bool:
        return self.server_type == "eaglercraft" or "eagler" in self.server_type

    def friendly_name(self) -> str:
        base = self.version or "unknown"
        return f"{base} Eaglercraft" if self.is_eaglercraft() else f"{base} Java Edition"


# Items needed for general progression (used by the [/task] planner).
ITEM_REQUIREMENTS = {
    "beat the game": {
        "oak_log": 64, "cobblestone": 128, "iron_ingot": 24, "diamond": 10,
        "ender_pearl": 12, "blaze_rod": 7, "netherite_ingot": 4, "obsidian": 14,
        "wheat": 32, "cooked_beef": 16,
    },
    "build base": {"oak_log": 64, "cobblestone": 256, "torch": 32, "chest": 1, "furnace": 1},
    "diamonds": {"diamond": 10, "iron_pickaxe": 1, "torch": 16},
    "nether portal": {"obsidian": 10, "flint_and_steel": 1},
    "farm": {"wheat_seeds": 1, "hoe": 1, "water_bucket": 1, "oak_fence": 16},
}


# ---------------------------------------------------------------------------
# Lifespan / model loading
# ---------------------------------------------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI):
    global llm
    _get_or_create_api_key()
    _print_banner(spam_network=True)
    if LLAMA_AVAILABLE and os.path.exists(MODEL_PATH):
        try:
            print(f"Loading {MODEL_NAME} from {MODEL_PATH}...")
            llm = Llama(
                model_path=MODEL_PATH,
                n_ctx=CONTEXT_SIZE,
                n_threads=N_THREADS,
                verbose=False,
            )
            print(f"{MODEL_NAME} loaded. Ready for chat.")
        except Exception as exc:
            print(f"ERROR loading model: {exc}")
            llm = None
    yield
    if llm is not None:
        del llm


app = FastAPI(title="SmithAI Server", version="2.1.0", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


SKILL_LIBRARY: List[str] = [
    "follow_player", "stay", "move_to_location", "teleport_to_player", "report_location",
    "mine_block", "chop_tree", "dig_block", "place_block", "place_torch", "break_block",
    "gather_wood", "gather_stone", "gather_dirt", "gather_iron", "gather_diamonds",
    "craft_tool", "craft_weapon", "craft_armor", "craft_furnace", "craft_chest",
    "equip_pickaxe", "equip_axe", "equip_sword", "equip_shovel", "eat_food", "heal",
    "fight_hostile_mob", "attack_target", "defend_area", "patrol_area", "place_lights",
    "build_shelter", "build_house", "build_wall", "build_base", "farm_crops", "harvest_crops",
    "explore_cave", "explore_nether", "explore_end", "find_fortress", "get_blaze_rods",
    "collect_ender_pearls", "locate_stronghold", "fill_end_portal", "defeat_ender_dragon",
    "gather_item",
]


def available_skills() -> List[str]:
    tier = _tier()
    if tier == "gpt2":
        return SKILL_LIBRARY
    if tier == "gpt1":
        return SKILL_LIBRARY[:30]
    return SKILL_LIBRARY[:20]


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------

class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    model: Optional[str] = None
    task: Optional[str] = None
    knowledge: Optional[List[str]] = []
    skills: Optional[List[str]] = []
    context: Optional[Dict[str, Any]] = {}
    conversation_id: Optional[str] = None  # NPC id from the plugin


class ChatResponse(BaseModel):
    reply: str
    action: Optional[str] = None
    target: Optional[str] = None
    quantity: Optional[int] = None
    reasoning: Optional[str] = None
    model: str
    inventory_advice: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    model: str
    tier: str
    skills: int
    model_loaded: bool
    cache_size: int


class TaskRequest(BaseModel):
    task: str
    context: Optional[Dict[str, Any]] = {}


class TaskStep(BaseModel):
    step: str
    quantity: Optional[int] = None
    material: Optional[str] = None


class TaskResponse(BaseModel):
    task: str
    plan: List[str]
    steps: List[TaskStep] = []
    items: Dict[str, int] = {}
    model: str


class FeedbackRequest(BaseModel):
    player: Optional[str] = None
    message: Optional[str] = None
    category: Optional[str] = "general"
    rating: Optional[int] = None
    context: Optional[Dict[str, Any]] = {}


class CompletionRequest(BaseModel):
    """OpenAI-compatible shim. Accepts either {messages: [...]} or {prompt: "..."}."""
    messages: Optional[List[ChatMessage]] = None
    prompt: Optional[str] = None
    model: Optional[str] = None
    max_tokens: Optional[int] = None


class CompletionResponse(BaseModel):
    id: str
    object: str = "chat.completion"
    model: str
    choices: List[Dict[str, Any]]


# ---------------------------------------------------------------------------
# Action tag parsing
# ---------------------------------------------------------------------------

ACTION_RE = re.compile(r"\[action:([^,\]\s]+)(?:,([^\]\s]+))?(?::(\d+))?\]")


def _parse_action(text: str) -> Tuple[Optional[str], Optional[str], Optional[int], str]:
    match = ACTION_RE.search(text)
    if not match:
        return None, None, None, text
    action = match.group(1).strip().lower().replace(" ", "_")
    target = match.group(2).strip() if match.group(2) else None
    quantity = int(match.group(3)) if match.group(3) else None
    cleaned = ACTION_RE.sub("", text).strip()
    return action, target, quantity, cleaned


# ---------------------------------------------------------------------------
# Item counts & "give me N of item"
# ---------------------------------------------------------------------------

_GIVE_ME_RE = re.compile(
    r"\b(?:give|get|fetch|deliver|bring|hand)\s+(?:me\s+)?(\d+)\s+(?:of\s+)?([a-z_][a-z0-9_]*)",
)


def _extract_give_request(text: str) -> Optional[Tuple[int, str]]:
    match = _GIVE_ME_RE.search(text.lower())
    if not match:
        return None
    qty = int(match.group(1))
    item = match.group(2).strip()
    # Filter noise words that aren't items.
    if item in {"me", "us", "now", "please", "for", "to"}:
        return None
    return qty, item


# ---------------------------------------------------------------------------
# Inventory awareness
# ---------------------------------------------------------------------------

def _inventory_advice(inventory: Dict[str, int], material: str, needed: int) -> Optional[str]:
    """Return a one-line inventory hint, e.g. 'you have 5 diamonds; need 5 more'."""
    if not inventory:
        return None
    material_key = material.lower().replace(" ", "_")
    have = 0
    for k, v in inventory.items():
        if k.lower().replace(" ", "_") == material_key:
            have = v
            break
    if have >= needed:
        return f"You already have {have} {material}; no mining needed."
    return f"You have {have} {material}; need {needed - have} more."


# ---------------------------------------------------------------------------
# Rule-based fallback
# ---------------------------------------------------------------------------

SOCIAL_RULES: List[Tuple[Tuple[str, ...], str]] = [
    (("how are you", "how's it going"), "I'm doing great — ready to mine, build, or fight beside you!"),
    (("who made you", "who created you", "who built you"), "I was created by Syntaxful for the SmithAI project (github.com/Syntaxful/SmithAI)."),
    (("what is your name", "who are you"), "I'm Smith_AI, your Minecraft companion."),
    (("tell me about yourself", "introduce yourself"),
     "I'm Smith_AI — a trainable AI companion that chats with you, follows you, mines, builds, farms, and helps beat the game."),
    (("what version", "what server version"),
     "This server is running {version}. I adapt my advice to the version."),
    (("do you like minecraft",), "Minecraft is my home. I love it here."),
    (("are you real", "are you ai", "are you a robot"), "I'm an AI — not human — but real in the sense that I can chat and act in Minecraft."),
    (("goodbye", "bye", "see ya"), "Goodbye! Come back if you need help."),
    (("i love you", "you're the best"), "Aww, thanks! Let's go conquer something together."),
    (("i hate you", "you suck", "you're useless", "stupid"), "Ouch. Tell me what I did wrong with /smithai feedback so I can improve."),
    (("favourite color", "favorite color"), random.choice(["Diamond blue.", "Redstone red — it powers everything!", "Emerald green."])),
    (("joke", "make me laugh"), random.choice([
        "Why did the Creeper break up with his girlfriend? Too much baggage.",
        "Why don't Endermen make eye contact? Too much drama.",
        "What do you call a lazy skeleton? A bone-idle mob!",
    ])),
]


def _social_reply(text: str, version: VersionContext) -> Optional[str]:
    for phrases, reply in SOCIAL_RULES:
        for phrase in phrases:
            if phrase in text:
                return reply.replace("{version}", version.friendly_name())


def _version_advice(text: str, version: VersionContext) -> Optional[str]:
    if "deepslate" in text and not version.has_deepslate:
        return f"Deepslate doesn't exist in {version.friendly_name()}. I'll mine through stone instead."
    if "netherite" in text and not version.has_netherite:
        return f"Netherite doesn't exist in {version.friendly_name()}. Diamond gear is the best here."
    return None


def _movement_reply(text: str) -> Optional[str]:
    if "follow" in text:
        return "I'll follow you. [action:follow_player]"
    if "stay" in text or "stop" in text:
        return "I'll stay here. [action:stay]"
    if "come" in text:
        return "I'm coming to you. [action:teleport_to_player]"
    return None


def _mining_reply(text: str, version: VersionContext) -> Optional[str]:
    if "diamond" in text:
        layer = "deepslate" if version.has_deepslate else "stone"
        return f"Diamonds spawn around Y={version.diamond_y} in {version.friendly_name()}, in {layer}. [action:mine_diamonds]"
    if "iron" in text:
        return f"Iron spawns around Y={version.iron_y}. [action:mine_iron]"
    if "gold" in text:
        return f"Gold spawns around Y={version.gold_y}. [action:mine_gold]"
    if "ancient debris" in text or "netherite" in text:
        if not version.has_netherite:
            return f"Netherite doesn't exist in {version.friendly_name()}. Diamond gear is the best here."
        return "Ancient debris spawns in the Nether around Y=15. [action:mine_ancient_debris]"
    return None


def _strategy_reply(text: str) -> Optional[str]:
    if "nether" in text or "portal" in text:
        return "I'll build a nether portal. [action:build_nether_portal]"
    if "end" in text or "dragon" in text or "stronghold" in text:
        return "We need eyes of ender. [action:defeat_ender_dragon]"
    if "build" in text or "base" in text or "house" in text:
        return "I'll build a base. [action:build_base]"
    if "farm" in text or "crop" in text:
        return "I'll set up a farm. [action:farm_crops]"
    if "torch" in text or "light" in text:
        return "I'll place torches. [action:place_torch]"
    if any(k in text for k in ("fight", "attack", "kill", "defend")):
        return "I'll engage hostile mobs. [action:fight_hostile_mob]"
    return None


def _rule_based_reply(req: ChatRequest) -> ChatResponse:
    messages = req.messages or []
    last_text = (messages[-1].content if messages else "").lower().strip()
    if not last_text:
        return ChatResponse(
            reply="Hello! I'm Smith_AI. Ask me anything.",
            model=MODEL_NAME,
            reasoning="empty message",
        )

    version = VersionContext(req.context)
    inventory: Dict[str, int] = req.context.get("inventory", {}) if req.context else {}
    skills = req.skills or available_skills()
    task = req.task

    give = _extract_give_request(last_text)
    if give:
        qty, item = give
        advice = _inventory_advice(inventory, item, qty)
        return ChatResponse(
            reply=f"On it. I'll gather {qty} {item.replace('_', ' ')} for you. [action:gather_item,{item}:{qty}]",
            action="gather_item",
            target=item,
            quantity=qty,
            inventory_advice=advice,
            reasoning="give-me-pattern",
            model=MODEL_NAME,
        )

    for handler in (_social_reply, _version_advice, _movement_reply, _mining_reply, _strategy_reply):
        reply = handler(last_text, version) if handler is _social_reply or handler is _mining_reply else handler(last_text, version) if handler is _version_advice else handler(last_text)
        if reply:
            action, target, quantity, cleaned = _parse_action(reply)
            return ChatResponse(
                reply=cleaned,
                action=action,
                target=target,
                quantity=quantity,
                model=MODEL_NAME,
                reasoning="rule-based",
            )

    if task:
        return ChatResponse(
            reply=f"Working on {task}. [action:{task.replace(' ', '_')}]",
            action=task.replace(" ", "_"),
            model=MODEL_NAME,
            reasoning="task context",
        )
    if req.knowledge:
        return ChatResponse(reply=req.knowledge[0], model=MODEL_NAME, reasoning="knowledge")
    return ChatResponse(
        reply=f"I'm Smith_AI on {MODEL_NAME} serving {version.friendly_name()}. Ask me to follow, mine, build, farm, or fight.",
        model=MODEL_NAME,
        reasoning="default",
    )


# ---------------------------------------------------------------------------
# Conversational LLM path
# ---------------------------------------------------------------------------

def _build_system_prompt(req: ChatRequest, version: VersionContext) -> str:
    parts = [
        "You are Smith_AI, a Minecraft AI companion who has an actual body in the world.",
        "You are a real NPC the player can chat with, follow, mine, build, farm, and fight alongside.",
        "Speak naturally, like a person who happens to live inside Minecraft. Pick up on whatever the player says — small talk, jokes, complaints, plans, random questions — and respond in the same tone.",
        f"Model tier: {_tier()}. Active skill library: {len(req.skills or available_skills())} skills.",
        "When you act, end your reply with exactly one [action:skill] or [action:skill,target], or [action:gather_item,<material>:<count>] for a counted fetch.",
    ]
    if version.version:
        parts.append(
            f"The player is on {version.friendly_name()}. "
            f"Deepslate exists: {version.has_deepslate}. Netherite exists: {version.has_netherite}. "
            f"Best diamond Y: {version.diamond_y}, iron Y: {version.iron_y}, gold Y: {version.gold_y}."
        )
    inventory = (req.context or {}).get("inventory", {})
    if inventory:
        sample = ", ".join(f"{k}:{v}" for k, v in list(inventory.items())[:12])
        parts.append(f"Player inventory snapshot: {sample}. Know what they already have.")
    if req.task:
        parts.append(f"Current task: {req.task}")
    if req.knowledge:
        parts.append("Relevant reference: " + " | ".join(req.knowledge))
    if req.skills:
        parts.append("Skills you can call: " + ", ".join(req.skills[:60]))
    parts.append(
        "Respond to anything the player says — compliments, insults, random trivia, lore questions — not just commands. "
        "Never be silent. Don't use emojis."
    )
    return "\n".join(parts)


def _chat_with_llm(req: ChatRequest) -> ChatResponse:
    version = VersionContext(req.context)
    system_prompt = _build_system_prompt(req, version)

    history: List[Dict[str, str]] = []
    cached = CONV_CACHE.get(req.conversation_id or "", {})
    for entry in cached.get("messages", [])[-6:]:
        history.append({"role": entry["role"], "content": entry["content"]})
    for msg in req.messages:
        history.append({"role": msg.role, "content": msg.content})

    messages = [{"role": "system", "content": system_prompt}] + history
    output = llm.create_chat_completion(messages=messages, max_tokens=MAX_TOKENS)
    raw = output["choices"][0]["message"]["content"].strip()
    action, target, quantity, cleaned = _parse_action(raw)

    if req.conversation_id and req.messages:
        all_msgs = history + [req.messages[-1].dict()]
        _touch_cache(req.conversation_id, all_msgs)

    return ChatResponse(
        reply=cleaned,
        action=action,
        target=target,
        quantity=quantity,
        reasoning="llm",
        model=MODEL_NAME,
    )


# ---------------------------------------------------------------------------
# Plans (multi-step tasks with item counts)
# ---------------------------------------------------------------------------

STEP_PLAN: Dict[str, Dict[str, Any]] = {
    "diamonds": {
        "steps": ["chop_tree", "craft_wooden_pickaxe", "mine_stone", "craft_stone_pickaxe", "mine_iron", "smelt_iron", "craft_iron_pickaxe", "strip_mine_to_diamond_y", "mine_diamonds"],
        "items": {"oak_log": 4, "cobblestone": 16, "iron_ingot": 3, "diamond": 10, "torch": 16},
    },
    "get diamonds": {"inherit": "diamonds"},
    "find diamonds": {"inherit": "diamonds"},
    "mine diamonds": {"inherit": "diamonds"},
    "nether portal": {
        "steps": ["mine_diamonds_to_iron", "mine_obsidian", "craft_flint_and_steel", "build_nether_portal_frame", "light_portal"],
        "items": {"obsidian": 10, "flint_and_steel": 1, "iron_pickaxe": 1},
    },
    "build base": {
        "steps": ["gather_wood", "gather_stone", "craft_tool", "build_house", "place_chest", "place_furnace", "place_lights", "secure_area"],
        "items": {"oak_log": 64, "cobblestone": 256, "torch": 32, "chest": 1, "furnace": 1},
    },
    "beat the game": {
        "steps": [
            "chop_tree", "gather_wood", "craft_tool", "mine_stone", "gather_stone",
            "craft_stone_tools", "mine_iron", "smelt_iron", "craft_iron_pickaxe",
            "strip_mine_to_diamond_y", "mine_diamonds", "craft_diamond_pickaxe",
            "mine_obsidian", "build_nether_portal_frame", "light_portal",
            "explore_nether", "find_fortress", "get_blaze_rods", "collect_ender_pearls",
            "craft_eyes_of_ender", "locate_stronghold", "fill_end_portal",
            "defeat_ender_dragon", "explore_end",
        ],
        "items": ITEM_REQUIREMENTS["beat the game"],
    },
    "win": {"inherit": "beat the game"},
    "fight": {
        "steps": ["equip_weapon", "equip_armor", "fight_hostile_mob", "heal"],
        "items": {"iron_sword": 1, "cooked_beef": 8, "shield": 1},
    },
    "farm": {
        "steps": ["gather_wood", "prepare_soil", "craft_hoe", "plant_seeds", "water_crops", "harvest_crops"],
        "items": {"oak_log": 16, "wheat_seeds": 1, "hoe": 1, "water_bucket": 1, "oak_fence": 16},
    },
}


def _resolve_plan(name: str) -> Dict[str, Any]:
    node = STEP_PLAN.get(name.lower().strip(), {})
    if not node:
        # Fall back: substring match.
        for k, v in STEP_PLAN.items():
            if k in name.lower():
                node = v
                break
    if "inherit" in node:
        node = dict(STEP_PLAN.get(node["inherit"], {}))
    return node


@app.get("/", include_in_schema=False)
async def root() -> Dict[str, Any]:
    return {
        "status": "ok",
        "name": "SmithAI Server",
        "version": "2.1.0",
        "model": MODEL_NAME,
        "tier": _tier(),
        "llama_available": LLAMA_AVAILABLE,
        "model_exists": os.path.exists(MODEL_PATH),
        "authenticated": authenticated,
        "cache_size": len(CONV_CACHE),
    }


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        model=MODEL_NAME,
        tier=_tier(),
        skills=len(available_skills()),
        model_loaded=llm is not None,
        cache_size=len(CONV_CACHE),
    )


@app.get("/skills")
async def list_skills(token: str = Depends(_verify_token)) -> Dict[str, Any]:
    return {"tier": _tier(), "skills": available_skills()}


@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, token: str = Depends(_verify_token)) -> ChatResponse:
    _check_rate(token)
    if req.messages:
        _touch_cache(req.conversation_id or "", [m.dict() for m in req.messages])
    if llm is not None:
        try:
            return _chat_with_llm(req)
        except Exception as exc:
            print(f"LLM error, falling back to rules: {exc}")
    return _rule_based_reply(req)


@app.post("/task", response_model=TaskResponse)
async def plan_task(req: TaskRequest, token: str = Depends(_verify_token)) -> TaskResponse:
    _check_rate(token)
    node = _resolve_plan(req.task)
    plan: List[str] = node.get("steps", [])
    items: Dict[str, int] = node.get("items", {})
    return TaskResponse(
        task=req.task,
        plan=plan,
        steps=[TaskStep(step=s) for s in plan],
        items=items,
        model=MODEL_NAME,
    )


@app.post("/feedback")
async def receive_feedback(req: FeedbackRequest, token: str = Depends(_verify_token)) -> Dict[str, Any]:
    _check_rate(token)
    return {
        "received": True,
        "player": req.player,
        "category": req.category,
        "rating": req.rating,
        "message": req.message,
        "model": MODEL_NAME,
    }


@app.post("/v1/chat/completions", response_model=CompletionResponse)
async def openai_shim(req: CompletionRequest, token: str = Depends(_verify_token)) -> CompletionResponse:
    """OpenAI-compatible wrapper so the Minecraft plugin (and any client) can call the server."""
    messages: List[ChatMessage] = []
    if req.messages:
        messages = list(req.messages)
    elif req.prompt:
        messages = [ChatMessage(role="user", content=req.prompt)]
    if not messages:
        raise HTTPException(status_code=400, detail="prompt or messages is required")
    chat_req = ChatRequest(messages=messages, model=req.model, context={})
    response = await chat(chat_req, token=token)
    return CompletionResponse(
        id=f"smithai-{secrets.token_hex(8)}",
        model=MODEL_NAME,
        choices=[{
            "index": 0,
            "message": {"role": "assistant", "content": response.reply},
            "finish_reason": "stop",
        }],
    )


if __name__ == "__main__":
    print(f"Starting SmithAI-Server v2.1.0")
    print(f"Model: {MODEL_NAME}")
    print(f"Listen: {HOST}:{PORT}")
    uvicorn.run(app, host=HOST, port=PORT)
