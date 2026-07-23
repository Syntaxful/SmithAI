# SmithAI-Server
# Official external AI server for SmithAI Minecraft plugin.
# Supports SmithGPT 1.0 (7.5GB) and SmithGPT 2.0 (15GB) models.
# User runs this on their own host: Replit, Codespaces, Linux, Windows, VPS, etc.

import os
import sys
import yaml
import secrets
import string
import asyncio
import uvicorn
import re
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Header, Depends, Request
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

MODEL_PATH = config.get("model", {}).get("path", "models/smithgpt-1.0-7.5.gguf")
MODEL_NAME = config.get("model", {}).get("name", "SmithGPT 1.0 7.5GB")
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
    description="External AI server for the SmithAI Minecraft plugin.",
    version="2.0.0",
    lifespan=lifespan,
)


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
    if "2.0" in MODEL_NAME or "15" in MODEL_NAME:
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
    system_prompt = (
        "You are Smith_AI, an AI companion in Minecraft. You can chat, help with tasks, "
        "and answer questions about the game. Keep replies short and useful.\n"
        f"Model tier: {model_tier}. Available core skills: {skill_count}.\n"
        "You may choose one or more skills to accomplish a task if it helps.\n"
        "If you choose a skill, end your reply with the tag [action:skill_name] or [action:skill_name,target].\n"
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

    reply = rule_reply(last, knowledge, skills, task)
    action, target, reply = parse_action_tag(reply)
    return ChatResponse(
        reply=reply,
        action=action,
        target=target,
        reasoning="Rule-based fallback",
        model=MODEL_NAME,
    )


def rule_reply(last: str, knowledge: List[str], skills: List[str], task: Optional[str]) -> str:
    if "follow" in last:
        return "I'll follow you. [action:follow_player]"
    if "stay" in last:
        return "I'll stay here. [action:stay]"
    if "diamond" in last:
        return "I'll mine for diamonds at Y=-59. [action:mine_block]"
    if "iron" in last:
        return "I'll find and mine iron ore. [action:mine_block]"
    if "portal" in last or "nether" in last:
        return "I'll build a nether portal. [action:build_nether_portal]"
    if "build" in last or "base" in last or "house" in last:
        return "I'll build a base. [action:build_base]"
    if "farm" in last or "crop" in last or "food" in last:
        return "I'll set up a farm. [action:farm_crops]"
    if "fight" in last or "kill" in last or "defend" in last:
        return "I'll engage hostile mobs. [action:fight_hostile_mob]"
    if "torch" in last or "light" in last:
        return "I'll place torches. [action:place_torch]"
    if task:
        return f"Working on {task}. [action:{task.replace(' ', '_')}]"
    if knowledge:
        return knowledge[0]
    return f"I'm Smith_AI on {MODEL_NAME}. Ask me to follow, mine, build, farm, or fight. ({len(skills)} skills available)"


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
    print(f"Starting SmithAI-Server v2.0.0")
    print(f"Model: {MODEL_NAME}")
    print(f"Listening on {HOST}:{PORT}")
    uvicorn.run(app, host=HOST, port=PORT)
