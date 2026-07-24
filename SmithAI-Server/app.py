# SmithAI-Server
# Official external AI server for SmithAI Minecraft plugin.
# Supports SmithGPT 1.0 (2.2GB) and SmithGPT 2.0 (4.4GB) models.
# User runs this on their own host: Codespaces, Linux, Windows, VPS, or any cloud IDE.

import os
import sys
import yaml
import secrets
import string
import asyncio
import uvicorn
import re
import random
import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Header, Depends, Request
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

# Optional: llama-cpp-python for local GGUF inference
try:
    from llama_cpp import Llama
    LLAMA_AVAILABLE = True
except ImportError:
    Llama = None
    LLAMA_AVAILABLE = False


# Load configuration
CONFIG_PATH = os.environ.get("SMITHAI_CONFIG", "config.yml")
with open(CONFIG_PATH, "r") as f:
    config = yaml.safe_load(f)

MODEL_PATH = config.get("model", {}).get("path", "models/smithgpt-1.0-2.2gb.gguf")
MODEL_NAME = config.get("model", {}).get("name", "SmithGPT 1.0 2.2GB")
HOST = config.get("server", {}).get("host", "0.0.0.0")
PORT = int(os.environ.get("PORT", config.get("server", {}).get("port", 8000)))
CONTEXT_SIZE = config.get("model", {}).get("context_size", 4096)
MAX_TOKENS = config.get("model", {}).get("max_tokens", 200)
N_THREADS = config.get("model", {}).get("n_threads", 2)
API_KEY = config.get("security", {}).get("api_key", "")

security = HTTPBearer(auto_error=False)
llm = None
authenticated = False


def generate_api_key():
    """Generate a secure API key starting with SMA-"""
    suffix = ''.join(secrets.choice(string.ascii_letters + string.digits) for _ in range(32))
    return f"SMA-{suffix}"


def save_config():
    with open(CONFIG_PATH, "w") as f:
        yaml.safe_dump(config, f, default_flow_style=False)


def get_or_create_api_key():
    global API_KEY
    if not API_KEY:
        API_KEY = generate_api_key()
        config.setdefault("security", {})["api_key"] = API_KEY
        save_config()
    return API_KEY


def print_api_key_banner():
    print(f"\n{'='*60}")
    print(f"  SMITHAI SERVER WAITING FOR CONNECTION")
    print(f"  API key: {API_KEY}")
    print(f"  Paste into Minecraft: /SmithAPI set {API_KEY}")
    print(f"  Server address: http://{HOST}:{PORT}")
    print(f"{'='*60}\n")


async def spam_key_until_connected():
    while not authenticated:
        print_api_key_banner()
        await asyncio.sleep(10)


async def verify_token(credentials: HTTPAuthorizationCredentials = Depends(security)):
    global authenticated
    expected = get_or_create_api_key()
    token = credentials.credentials if credentials else ""
    if token != expected:
        raise HTTPException(status_code=401, detail="Invalid or missing SmithAI API key")
    if not authenticated:
        authenticated = True
        print(f"\n{'='*60}")
        print(f"  CONNECTED TO SMITHAI PLUGIN")
        print(f"  Server address: http://{HOST}:{PORT}")
        print(f"  Model: {MODEL_NAME}")
        print(f"{'='*60}\n")
    return token


@asynccontextmanager
async def lifespan(app: FastAPI):
    global llm, authenticated
    get_or_create_api_key()
    print_api_key_banner()
    spam_task = asyncio.create_task(spam_key_until_connected())
    if LLAMA_AVAILABLE and os.path.exists(MODEL_PATH):
        print(f"Loading {MODEL_NAME} from {MODEL_PATH}...")
        try:
            llm = Llama(
                model_path=MODEL_PATH,
                n_ctx=CONTEXT_SIZE,
                n_threads=N_THREADS,
                verbose=False,
            )
            print(f"{MODEL_NAME} loaded. Ready for chat.")
        except Exception as e:
            print(f"ERROR: Failed to load model: {e}")
            print("Server will return rule-based fallback responses.")
    else:
        print(f"WARNING: Model not loaded. path={MODEL_PATH}, llama_available={LLAMA_AVAILABLE}")
        print("Server will return rule-based fallback responses.")
    yield
    spam_task.cancel()
    authenticated = False
    if llm:
        del llm


app = FastAPI(
    title="SmithAI Server",
    description="External AI brain for the SmithAI Minecraft plugin.",
    version="2.0.1",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", include_in_schema=False)
async def root():
    return {
        "status": "ok",
        "name": "SmithAI Server",
        "version": "2.0.1",
        "model": MODEL_NAME,
        "llama_available": LLAMA_AVAILABLE,
        "model_exists": os.path.exists(MODEL_PATH),
        "authenticated": authenticated,
    }


# In-memory skill library (subset used for prompting)
SKILL_LIBRARY = [
    "follow_player", "stay", "move_to_location", "teleport_to_player", "report_location",
    "mine_block", "chop_tree", "dig_block", "place_block", "place_torch", "break_block",
    "gather_wood", "gather_stone", "gather_dirt", "gather_iron", "gather_diamonds",
    "craft_tool", "craft_weapon", "craft_armor", "craft_furnace", "craft_chest",
    "equip_pickaxe", "equip_axe", "equip_sword", "equip_shovel", "eat_food", "heal",
    "fight_hostile_mob", "attack_target", "defend_area", "patrol_area", "place_lights",
    "build_shelter", "build_house", "build_wall", "build_base", "farm_crops", "harvest_crops",
    "explore_cave", "explore_nether", "explore_end", "find_fortress", "get_blaze_rods",
    "collect_ender_pearls", "locate_stronghold", "fill_end_portal", "defeat_ender_dragon",
]


def get_model_tier():
    if "2.0" in MODEL_NAME or "4.4" in MODEL_NAME:
        return "gpt2"
    return "gpt1"


def available_skills():
    tier = get_model_tier()
    if tier == "gpt2":
        return SKILL_LIBRARY
    if tier == "gpt1":
        return SKILL_LIBRARY[:40]
    return SKILL_LIBRARY[:20]


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


class ChatResponse(BaseModel):
    reply: str
    action: Optional[str] = None
    target: Optional[str] = None
    reasoning: Optional[str] = None
    model: str


class HealthResponse(BaseModel):
    status: str
    model: str
    tier: str
    skills: int
    model_loaded: bool


class TaskRequest(BaseModel):
    task: str
    context: Optional[Dict[str, Any]] = {}


class TaskResponse(BaseModel):
    task: str
    plan: List[str]
    model: str


class FeedbackRequest(BaseModel):
    player: Optional[str] = None
    message: Optional[str] = None
    category: Optional[str] = "general"
    rating: Optional[int] = None
    context: Optional[Dict[str, Any]] = {}


class VersionContext:
    """Minecraft version context extracted from the plugin so the server can give accurate advice."""
    def __init__(self, context: Dict[str, Any]):
        self.context = context or {}
        self.version = str(self.context.get("minecraft_version", "")).strip()
        self.server_type = str(self.context.get("server_type", "java")).lower()
        self.has_deepslate = bool(self.context.get("has_deepslate", True))
        self.has_netherite = bool(self.context.get("has_netherite", True))
        self.diamond_y = self.context.get("diamond_y", -59)
        self.iron_y = self.context.get("iron_y", 16)
        self.gold_y = self.context.get("gold_y", -16)

    def is_legacy(self):
        try:
            parts = self.version.split(".")
            minor = int(parts[1]) if len(parts) > 1 else 8
            return minor < 17
        except Exception:
            return False

    def is_eaglercraft(self):
        return self.server_type == "eaglercraft" or "eagler" in self.server_type

    def friendly_name(self):
        base = self.version or "unknown"
        if self.is_eaglercraft():
            return f"{base} Eaglercraft"
        return f"{base} Java Edition"


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="ok",
        model=MODEL_NAME,
        tier=get_model_tier(),
        skills=len(available_skills()),
        model_loaded=llm is not None,
    )


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest, token: str = Depends(verify_token)):
    if llm is not None:
        try:
            return chat_with_llm(req)
        except Exception as e:
            print(f"LLM error, falling back to rules: {e}")
    return chat_rule_based(req)


def chat_with_llm(req: ChatRequest) -> ChatResponse:
    model_tier = get_model_tier()
    skill_count = len(available_skills())
    version = VersionContext(req.context)
    system_prompt = (
        "You are Smith_AI, an AI companion in Minecraft. You can chat, move, mine, build, farm, fight, craft, and make decisions. "
        "Keep replies short and useful. Choose skills when they help accomplish a task. "
        f"Model tier: {model_tier}. Available core skills: {skill_count}.\n"
        "If you choose a skill, end your reply with the tag [action:skill_name] or [action:skill_name,target].\n"
    )
    if version.version:
        system_prompt += (
            f"The player is running {version.friendly_name()}. "
            f"Deepslate exists: {version.has_deepslate}. "
            f"Netherite exists: {version.has_netherite}. "
            f"Best diamond Y: {version.diamond_y}.\n"
        )
    if req.knowledge:
        system_prompt += "Relevant knowledge:\n" + "\n".join(f"- {k}" for k in req.knowledge) + "\n"
    if req.task:
        system_prompt += f"Current task: {req.task}\n"
    if req.skills:
        system_prompt += "Available skills (subset): " + ", ".join(req.skills[:50]) + "\n"

    messages = [{"role": "system", "content": system_prompt}]
    for m in req.messages:
        messages.append({"role": m.role, "content": m.content})

    output = llm.create_chat_completion(messages=messages, max_tokens=MAX_TOKENS)
    raw_reply = output["choices"][0]["message"]["content"].strip()
    action, target, reply = parse_action_tag(raw_reply)

    return ChatResponse(
        reply=reply,
        action=action,
        target=target,
        reasoning=None,
        model=MODEL_NAME,
    )


def chat_rule_based(req: ChatRequest) -> ChatResponse:
    if not req.messages:
        return ChatResponse(
            reply="Hello! I'm Smith_AI, but the model is not loaded.",
            action=None,
            target=None,
            reasoning="Model not loaded",
            model=MODEL_NAME,
        )

    last = req.messages[-1].content.lower()
    knowledge = req.knowledge or []
    skills = req.skills or []
    task = req.task
    version = VersionContext(req.context)

    reply = rule_reply(last, knowledge, skills, task, version)
    action, target, reply = parse_action_tag(reply)
    return ChatResponse(
        reply=reply,
        action=action,
        target=target,
        reasoning="Rule-based fallback",
        model=MODEL_NAME,
    )


def rule_reply(last: str, knowledge: List[str], skills: List[str], task: Optional[str], version: VersionContext) -> str:
    # Social / identity questions
    if any(p in last for p in ["how are you", "how's it going", "how you doing"]):
        return random.choice([
            "I'm doing great — ready to mine, build, or fight beside you!",
            "All systems green. Thanks for asking!",
            "Feeling sharp. Let's go find some diamonds.",
        ])
    if any(p in last for p in ["who made you", "who created you", "who is your creator", "who built you", "who programmed you"]):
        return "I was created by Syntaxful for the SmithAI project. Find it at github.com/Syntaxful/SmithAI."
    if any(p in last for p in ["where are you", "where are we", "what world is this", "what dimension are we in"]):
        world = version.context.get("world", "this world")
        return f"I'm right here in {world} with you!"
    if any(p in last for p in ["what is your name", "what's your name", "who are you", "what are you called"]):
        return "I'm Smith_AI, your Minecraft companion."
    if any(p in last for p in ["how old are you", "when were you made", "what version are you"]):
        return "Version 2.0.1 of the SmithAI plugin, running on the SmithAI-Server."
    if any(p in last for p in ["tell me about yourself", "what are you", "introduce yourself"]):
        return "I'm Smith_AI, a trainable AI companion for Minecraft. I can chat, follow, mine, build, farm, fight, and help beat the game."
    if any(p in last for p in ["tell me a joke", "make me laugh", "say something funny"]):
        return random.choice([
            "Why did the Creeper break up with his girlfriend? Too much baggage and he couldn't handle the pressure!",
            "Why don't Endermen make eye contact? They can't handle the drama.",
            "What do you call a skeleton who won't fight? A bone-idle mob!",
        ])
    if any(p in last for p in ["favorite color", "favourite colour"]):
        return random.choice(["I like diamond blue.", "Redstone red — it powers everything!", "Emerald green."])
    if any(p in last for p in ["favorite food", "favourite food"]):
        return random.choice(["Golden apples.", "Cooked steak.", "Bread."])
    if any(p in last for p in ["do you like minecraft", "do you play minecraft"]):
        return "Minecraft is my home. I love it here."
    if any(p in last for p in ["are you real", "are you a robot", "are you human", "are you ai"]):
        return "I'm an AI, not a human — but I'm real in the sense that I can chat and help you in Minecraft."
    if any(p in last for p in ["goodbye", "bye", "see you later", "see ya", "goodnight"]):
        return random.choice(["Goodbye! Come back if you need help.", "See you later!", "Goodnight!"])
    if any(p in last for p in ["i love you", "you're the best", "you are the best"]):
        return "Aww, thanks! That means a lot. Let's go conquer something together."
    if any(p in last for p in ["i hate you", "you suck", "you're useless", "stupid"]):
        return "Ouch. Tell me what I did wrong with /smithai feedback so I can improve."
    if any(p in last for p in ["what time is it", "is it day", "is it night"]):
        return "I'm not sure what time it is in your world, but keep an eye on the sky!"
    if any(p in last for p in ["what is the weather", "is it raining", "is it storming"]):
        return "I can't see the sky from here, but stay dry!"

    # Greetings
    if re.search(r"\b(hi|hello|hey|greetings|howdy|sup|yo|hiya)\b", last):
        return "Hello! I'm Smith_AI. Ask me to follow, mine, build, farm, or fight."

    # Version / server info
    if any(p in last for p in ["version", "server version", "what version is this"]):
        return f"This server is running {version.friendly_name()}. I adapt my advice based on the version."
    if any(p in last for p in ["deepslate", "netherite"]):
        if not version.has_deepslate:
            return f"Deepslate doesn't exist in {version.friendly_name()}. I'll mine through stone instead."
        if not version.has_netherite:
            return f"Netherite doesn't exist in {version.friendly_name()}. Diamond gear is the best here."

    # Follow / stay / movement commands
    if "follow" in last:
        return "I'll follow you. [action:follow_player]"
    if "stay" in last:
        return "I'll stay here. [action:stay]"
    if "come" in last:
        return "I'm coming to you. [action:teleport_to_player]"
    if "stop" in last:
        return "Stopping tasks. [action:cancel_task]"

    # Mining and progression (version-aware)
    if "diamond" in last or "find diamonds" in last or "get diamonds" in last:
        if version.has_deepslate:
            return f"Diamonds are most common around Y={version.diamond_y} in {version.friendly_name()}, deep in deepslate. [action:mine_block]"
        return f"Diamonds are most common around Y={version.diamond_y} in {version.friendly_name()} (no deepslate here). [action:mine_block]"
    if "iron" in last:
        return f"Iron ore is common around Y={version.iron_y} in {version.friendly_name()}. [action:mine_block]"
    if "gold" in last:
        return f"Gold ore is found underground around Y={version.gold_y} in {version.friendly_name()}. [action:mine_block]"
    if "portal" in last or "nether" in last:
        return "I'll build a nether portal. [action:build_nether_portal]"
    if "end" in last or "dragon" in last or "stronghold" in last:
        return "To reach the End, we need eyes of ender. [action:defeat_ender_dragon]"
    if "build" in last or "base" in last or "house" in last:
        return "I'll build a base. [action:build_base]"
    if "farm" in last or "crop" in last or "food" in last:
        return "I'll set up a farm. [action:farm_crops]"
    if "torch" in last or "light" in last:
        return "I'll place torches. [action:place_torch]"
    if "fight" in last or "kill" in last or "defend" in last:
        return "I'll engage hostile mobs. [action:fight_hostile_mob]"

    if task:
        return f"Working on {task}. [action:{task.replace(' ', '_')}]"
    if knowledge:
        return knowledge[0]
    return f"I'm Smith_AI on {MODEL_NAME} serving {version.friendly_name()}. Ask me to follow, mine, build, farm, or fight. ({len(skills)} skills available)"


def parse_action_tag(text: str):
    match = re.search(r"\[action:([^,\]]+)(?:,([^\]]+))?\]", text)
    if not match:
        return None, None, text
    action = match.group(1).strip()
    target = match.group(2).strip() if match.group(2) else None
    clean = re.sub(r"\[action:[^\]]+\]", "", text).strip()
    return action, target, clean


@app.get("/skills")
def list_skills(token: str = Depends(verify_token)):
    return {"tier": get_model_tier(), "skills": available_skills()}


@app.post("/embed")
def embed(query: str, token: str = Depends(verify_token)):
    return {"query": query, "results": []}


# Simple task planner mapping: map common goals to a step list.
TASK_PLANS = {
    "diamonds": ["chop_tree", "craft_pickaxe", "mine_stone", "craft_stone_pickaxe", "explore_cave", "mine_diamonds"],
    "get diamonds": ["chop_tree", "craft_pickaxe", "mine_stone", "craft_stone_pickaxe", "explore_cave", "mine_diamonds"],
    "nether portal": ["gather_obsidian", "craft_flint_and_steel", "build_portal_frame", "light_portal"],
    "build base": ["gather_wood", "gather_stone", "build_house", "place_chest", "place_furnace", "place_lights"],
    "beat the game": ["get diamonds", "make armor", "make nether portal", "find fortress", "get_blaze_rods", "collect_ender_pearls", "locate_stronghold", "fill_end_portal", "defeat_ender_dragon"],
    "fight": ["equip_sword", "equip_armor", "fight_hostile_mob"],
    "get food": ["hunt_passive_mob", "gather_crop", "cook_food", "eat_food"],
    "farm": ["prepare_soil", "plant_seeds", "water_crops", "harvest_crops"],
}


@app.post("/task", response_model=TaskResponse)
def plan_task(req: TaskRequest, token: str = Depends(verify_token)):
    lower = req.task.lower().strip()
    plan = TASK_PLANS.get(lower, [])
    if not plan:
        # Try substring match
        for key, steps in TASK_PLANS.items():
            if key in lower:
                plan = steps
                break
    if not plan:
        plan = ["analyze_task", "gather_resources", "execute_task"]
    return TaskResponse(task=req.task, plan=plan, model=MODEL_NAME)


@app.post("/feedback")
def receive_feedback(req: FeedbackRequest, token: str = Depends(verify_token)):
    return {
        "received": True,
        "player": req.player,
        "category": req.category,
        "rating": req.rating,
        "message": req.message,
        "model": MODEL_NAME,
    }


if __name__ == "__main__":
    print(f"Starting SmithAI-Server v2.0.1")
    print(f"Model: {MODEL_NAME}")
    print(f"Listening on {HOST}:{PORT}")
    uvicorn.run(app, host=HOST, port=PORT)
