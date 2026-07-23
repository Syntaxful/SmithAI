# SmithAI-Server
# Official external AI server for SmithAI Minecraft plugin.
# Supports SmithGPT 1.0 (4GB) and SmithGPT 2.0 (7.5GB) models.
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
import logging
from logging.handlers import RotatingFileHandler
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Header, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
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

MODEL_PATH = config.get("model", {}).get("path", "models/smithgpt-1.0-4.gguf")
MODEL_NAME = config.get("model", {}).get("name", "SmithGPT 1.0 4GB")
HOST = config.get("server", {}).get("host", "0.0.0.0")
PORT = int(os.environ.get("PORT", config.get("server", {}).get("port", 8000)))
CONTEXT_SIZE = config.get("model", {}).get("context_size", 4096)
MAX_TOKENS = config.get("model", {}).get("max_tokens", 200)
N_THREADS = config.get("model", {}).get("n_threads", 2)
API_KEY = config.get("security", {}).get("api_key", "")

security = HTTPBearer(auto_error=False)
llm = None
authenticated = False
logger = logging.getLogger("smithai-server")
os.makedirs("logs", exist_ok=True)
log_handler = RotatingFileHandler("logs/smithai-server.log", maxBytes=5*1024*1024, backupCount=3)
log_handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(message)s"))
logger.addHandler(log_handler)
logger.setLevel(logging.INFO)


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
            print(f"{MODEL_NAME} loaded. Warming up model...")
            try:
                llm("Hello, I am Smith_AI. How can I help you?", max_tokens=10, temperature=0.1)
                print(f"{MODEL_NAME} warmup complete. Ready for chat.")
            except Exception as e:
                print(f"Model warmup failed (non-fatal): {e}")
                print("Server will still attempt inference on requests.")
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
    logger.info("SmithAI-Server shutdown complete.")


app = FastAPI(
    title="SmithAI Server",
    description="External AI brain for the SmithAI Minecraft plugin.",
    version="2.0.0",
    lifespan=lifespan,
)


@app.get("/", include_in_schema=False)
async def root():
    return {
        "status": "ok",
        "name": "SmithAI Server",
        "version": "2.0.0",
        "model": MODEL_NAME,
        "llama_available": LLAMA_AVAILABLE,
        "model_exists": os.path.exists(MODEL_PATH),
        "authenticated": authenticated,
    }


# ========== PROMPT TEMPLATES ==========
PROMPT_TEMPLATES = {
    "gpt1": {
        "system": "You are Smith_AI, a helpful Minecraft companion. You can chat, help with tasks, and answer Minecraft questions. Keep replies concise and useful. End with [action:skill_name] if a skill is appropriate.",
        "version_context": True,
        "knowledge_prefix": "Relevant information:\n",
        "max_tokens": 200,
    },
    "gpt2": {
        "system": "You are Smith_AI, an advanced Minecraft AI companion with strategic reasoning. You break complex goals into steps. Use skills from your library. Consider resources, environment, and player safety. You can reason about multi-step processes and coordinate advanced tasks.",
        "version_context": True,
        "knowledge_prefix": "Strategy context:\n",
        "max_tokens": 400,
    },
}

# ========== RATE LIMITING ==========
import time
from collections import defaultdict
RATE_LIMIT_RPS = float(config.get("server", {}).get("rate_limit", 10.0))
RATE_WINDOW = 1.0
rate_tracker = defaultdict(list)

def check_rate_limit(client_ip: str):
    now = time.time()
    window = rate_tracker[client_ip]
    # Remove old entries
    while window and window[0] < now - RATE_WINDOW:
        window.pop(0)
    if len(window) >= RATE_LIMIT_RPS:
        raise HTTPException(status_code=429, detail=f"Rate limit exceeded ({RATE_LIMIT_RPS} req/s)")
    window.append(now)

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
    if "2.0" in MODEL_NAME:
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


class EmbedRequest(BaseModel):
    query: str


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


@app.get("/status", response_class=HTMLResponse)
def status_page():
    model_loaded = llm is not None
    tier = get_model_tier()
    mem = 0
    try:
        import psutil
        mem = psutil.Process().memory_info().rss // (1024*1024)
    except ImportError:
        pass
    skills = available_skills()
    html = f"""<!DOCTYPE html><html><head><title>SmithAI-Server Status</title>
<style>body{{font-family:sans-serif;max-width:720px;margin:40px auto;padding:0 20px;background:#1a1a2e;color:#eee}}
h1{{color:#e94560}}table{{width:100%;border-collapse:collapse}}th,td{{padding:8px 12px;text-align:left;border-bottom:1px solid #333}}
th{{color:#e94560;font-weight:normal}}.ok{{color:#4ecca3}}.warn{{color:#ffc93c}}.bad{{color:#e94560}}
pre{{background:#16213e;padding:12px;border-radius:6px;overflow-x:auto}}</style></head>
<body><h1>SmithAI-Server</h1><table>
<tr><th>Status</th><td class="ok">Running</td></tr>
<tr><th>Model</th><td>{MODEL_NAME}</td></tr>
<tr><th>Tier</th><td>{tier}</td></tr>
<tr><th>Model Loaded</th><td class="{'ok' if model_loaded else 'warn'}">{'Yes' if model_loaded else 'No (fallback)'}</td></tr>
<tr><th>Authenticated</th><td class="{'ok' if authenticated else 'warn'}">{'Yes' if authenticated else 'Waiting for plugin'}</td></tr>
<tr><th>Skills Available</th><td>{len(skills)}</td></tr>
<tr><th>Memory Usage</th><td>{mem} MB</td></tr>
<tr><th>Llama Available</th><td>{'Yes' if LLAMA_AVAILABLE else 'No'}</td></tr>
</table><h2>Available Skills ({len(skills)})</h2><pre>{chr(10).join(skills)}</pre>
<p style="color:#666;margin-top:40px">SmithAI-Server v2.0.0</p></body></html>"""
    return HTMLResponse(html)

@app.get("/health")
def health(request: Request):
    rss_mb = 0
    try:
        import psutil
        rss_mb = psutil.Process().memory_info().rss // (1024 * 1024)
    except ImportError:
        pass
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "tier": get_model_tier(),
        "skills": len(available_skills()),
        "model_loaded": llm is not None,
        "authenticated": authenticated,
        "memory_mb": rss_mb,
        "rate_limit_rps": RATE_LIMIT_RPS,
        "prompt_templates": list(PROMPT_TEMPLATES.keys()),
    }


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest, token: str = Depends(verify_token), request: Request = None):
    if request:
        client = request.client.host if request.client else "unknown"
        check_rate_limit(client)
    if llm is not None:
        try:
            return chat_with_llm(req)
        except Exception as e:
            print(f"LLM error, falling back to rules: {e}")
            logger.warning(f"LLM error: {e}")
    return chat_rule_based(req)


def chat_with_llm(req: ChatRequest) -> ChatResponse:
    model_tier = get_model_tier()
    tpl = get_prompt_template(model_tier)
    skill_count = len(available_skills())
    version = VersionContext(req.context)
    system_prompt = tpl["system"] + f"\nModel tier: {model_tier}. Available core skills: {skill_count}.\n"
    system_prompt += "If you choose a skill, end your reply with the tag [action:skill_name] or [action:skill_name,target].\n"
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

    output = llm.create_chat_completion(messages=messages, max_tokens=tpl.get("max_tokens", MAX_TOKENS))
    raw_reply = output["choices"][0]["message"]["content"].strip()
    action, target, reply = parse_action_tag(raw_reply)

    return ChatResponse(
        reply=reply,
        action=action,
        target=target,
        reasoning=None,
        model=MODEL_NAME,
    )


def get_prompt_template(tier: str) -> Dict[str, Any]:
    return PROMPT_TEMPLATES.get(tier, PROMPT_TEMPLATES["gpt1"])

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
        return "Version 2.0.0 of the SmithAI plugin, running on the SmithAI-Server."
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
def embed_ep(req: EmbedRequest, token: str = Depends(verify_token)):
    query = req.query
    """Keyword-based knowledge embedding. Returns relevant knowledge entries matching the query."""
    q = query.lower().strip()
    if not q:
        return {"query": query, "results": [], "count": 0}

    query_words = set(re.findall(r'\w+', q))
    if not query_words:
        return {"query": query, "results": [], "count": 0}

    # Load embedded knowledge
    kb_path = os.path.join(os.path.dirname(__file__) or ".", "knowledge")
    if not os.path.exists(kb_path):
        os.makedirs(kb_path, exist_ok=True)

    results = []
    if os.path.exists(kb_path):
        for fname in os.listdir(kb_path):
            if fname.endswith(".json"):
                try:
                    with open(os.path.join(kb_path, fname)) as f:
                        data = json.load(f)
                    if isinstance(data, list):
                        for entry in data:
                            text = f"{entry.get('name','')} {entry.get('description','')} {entry.get('category','')}"
                            words = set(re.findall(r'\w+', text.lower()))
                            overlap = len(query_words & words)
                            if overlap > 0:
                                results.append((overlap, {
                                    "id": entry.get("id", ""),
                                    "name": entry.get("name", ""),
                                    "description": entry.get("description", ""),
                                    "category": entry.get("category", ""),
                                    "match_score": overlap
                                }))
                except Exception:
                    continue

    results.sort(key=lambda x: -x[0])
    top = [r for _, r in results[:5]]

    return {
        "query": query,
        "results": top,
        "count": len(top),
        "method": "keyword_overlap"
    }


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


@app.get("/rl_data")
def rl_data():
    """Return RL training data stats from the plugin's CSV file."""
    return {
        "status": "ok",
        "note": "Full RL data is stored as a CSV file (rl_data.csv) in the plugin's data folder.",
        "model": MODEL_NAME,
        "fields": ["ts (unix ms)", "type (R=reward, P=punishment)", "action", "score (cumulative)"],
        "examples": []
    }


@app.get("/rl_data/health")
def rl_data_health():
    return {
        "status": "ok",
        "events_recorded": "Check plugin's rl_data.csv file.",
        "model": MODEL_NAME,
    }


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
    print(f"Starting SmithAI-Server v2.0.0")
    print(f"Model: {MODEL_NAME}")
    print(f"Listening on {HOST}:{PORT}")
    logger.info(f"Starting SmithAI-Server v2.0.0 (model={MODEL_NAME}, host={HOST}, port={PORT})")
    uvicorn.run(app, host=HOST, port=PORT, log_config=None)
