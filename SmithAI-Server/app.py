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
        llm = Llama(
            model_path=MODEL_PATH,
            n_ctx=CONTEXT_SIZE,
            n_threads=N_THREADS,
            verbose=False,
        )
        print(f"{MODEL_NAME} loaded. Ready for chat.")
    else:
        print(f"WARNING: Model not loaded. path={MODEL_PATH}, llama_available={LLAMA_AVAILABLE}")
        print("Server will return rule-based fallback responses.")
    yield
    spam_task.cancel()
    authenticated = False
    if llm:
        del llm


app = FastAPI(
    title="SmithAI-Server",
    description="External AI server for the SmithAI Minecraft plugin.",
    version="2.0.0",
    lifespan=lifespan,
)


class ChatMessage(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[ChatMessage]
    context: Optional[Dict[str, Any]] = {}
    task: Optional[str] = None
    knowledge: Optional[List[str]] = []
    skills: Optional[List[str]] = []
    model: Optional[str] = "smithgpt-1.0-7.5"


class ChatResponse(BaseModel):
    reply: str
    action: Optional[str] = None
    target: Optional[str] = None
    reasoning: Optional[str] = None
    model: str


@app.get("/health")
def health():
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "model_loaded": llm is not None,
        "model_path": MODEL_PATH,
        "server_address": f"http://{HOST}:{PORT}",
    }


@app.post("/chat", response_model=ChatResponse)
def get_model_tier():
    name = MODEL_NAME.lower()
    if "mini" in name:
        return "mini"
    if "2.0" in name or "15" in name or "gpt2" in name:
        return "gpt2"
    return "gpt1"


def available_skills():
    tier = get_model_tier()
    mini = [
        "greet", "farewell", "thank", "apologize", "ask", "answer", "joke", "compliment", "warn", "encourage",
        "praise", "scold", "narrate", "describe", "summarize", "translate", "repeat", "remember", "recall", "forget",
        "report_status", "wait", "noop", "think", "suggest", "advise", "follow", "stay", "move", "turn",
        "look", "jump", "sneak", "sprint", "swim", "climb", "inspect", "use", "equip", "drop",
        "pick_up", "interact", "open", "close", "sleep", "wake", "eat", "heal", "avoid", "retreat",
        "explore", "set_home", "go_home", "teleport_to_player", "teleport_to_spawn", "report_location", "wave", "bow", "salute", "dance",
        "emote", "pose", "gesture", "point", "call", "whisper", "set_mood", "focus", "relax", "alert",
        "calm", "celebrate", "mourn", "agree", "disagree", "offer_help", "decline", "accept", "invite", "welcome",
        "bid_farewell", "introduce", "explain_self", "state_name", "state_role", "state_model", "count", "compare", "estimate", "predict",
        "plan_simple", "schedule", "prioritize", "organize", "prepare", "cleanup", "check_inventory", "check_health", "check_hunger", "check_time",
        "select_tool", "select_food", "select_weapon", "select_block", "select_item", "save_favorite", "load_favorite", "clear_favorite", "set_nickname", "get_nickname",
        "begin_task", "end_task", "pause_task", "resume_task", "cancel_task", "retry_task", "skip_step", "repeat_step", "mark_done", "mark_failed",
        "say_hello", "say_goodbye", "say_yes", "say_no", "say_maybe", "say_please", "say_thanks", "say_sorry", "say_help", "say_ready",
        "ask_name", "ask_status", "ask_location", "ask_goal", "ask_opinion", "ask_preference", "ask_permission", "ask_for_help", "ask_for_item", "ask_for_direction",
        "tell_story", "tell_joke", "tell_fact", "tell_tip", "tell_warning", "tell_progress", "tell_plan", "tell_result", "tell_error", "tell_success",
        "recall_player", "recall_place", "recall_item", "recall_event", "recall_task", "recall_preference", "recall_order", "recall_joke", "recall_fact", "recall_tip",
        "focus_attention", "ignore_distraction", "reset_attention", "track_player", "track_mob", "track_item", "track_location", "track_time", "track_weather", "track_task"
    ]
    gpt1 = [
        "gather_wood", "gather_stone", "gather_coal", "gather_iron", "gather_copper", "gather_gold", "gather_diamond", "gather_emerald", "gather_redstone", "gather_lapis",
        "gather_obsidian", "gather_dirt", "gather_sand", "gather_gravel", "gather_clay", "gather_wool", "gather_leather", "gather_meat", "gather_fish", "gather_crop",
        "gather_flower", "gather_mushroom", "gather_berry", "gather_honey", "gather_sapling", "gather_seed", "gather_log", "gather_plank", "gather_stick", "gather_cobblestone",
        "craft_tool", "craft_weapon", "craft_armor", "craft_food", "craft_block", "craft_item", "craft_potion", "craft_torch", "craft_ladder", "craft_bed",
        "craft_crafting_table", "craft_furnace", "craft_chest", "craft_door", "craft_fence", "craft_wall", "craft_bridge", "craft_shelter", "craft_house", "craft_room",
        "fight_hostile_mob", "fight_passive_mob", "fight_neutral_mob", "fight_boss", "fight_prey", "fight_predator", "fight_threat", "fight_target", "fight_enemy", "fight_intruder",
        "farm_wheat", "farm_carrot", "farm_potato", "farm_beetroot", "farm_melon", "farm_pumpkin", "farm_cocoa", "farm_cane", "farm_cactus", "farm_bamboo",
        "farm_cow", "farm_pig", "farm_chicken", "farm_sheep", "farm_rabbit", "farm_horse", "farm_wolf", "farm_cat", "farm_villager", "farm_trader",
        "explore_cave", "explore_village", "explore_forest", "explore_mountain", "explore_river", "explore_ocean", "explore_desert", "explore_jungle", "explore_plains", "explore_hill",
        "store_items", "sort_items", "organize_inventory", "deposit_items", "withdraw_items", "stack_items", "transfer_items", "trade_items", "stock_items", "manage_items",
        "use_tool", "use_weapon", "use_item", "use_block", "consume", "activate", "deactivate", "toggle", "configure", "prepare"
    ]
    gpt2 = [
        "conquer_nether", "conquer_end", "conquer_stronghold", "conquer_fortress", "conquer_bastion", "conquer_end_city", "conquer_dragon", "conquer_wither", "conquer_warden", "conquer_ancient_city",
        "master_nether", "master_end", "master_portal", "master_combat", "master_building", "master_redstone", "master_enchanting", "master_brewing", "master_farming", "master_trading",
        "optimize_automation", "optimize_mining", "optimize_farming", "optimize_storage", "optimize_crafting", "optimize_trading", "optimize_defense", "optimize_exploration", "optimize_resource", "optimize_path",
        "automate_mining", "automate_farming", "automate_crafting", "automate_smelting", "automate_brewing", "automate_trading", "automate_defense", "automate_mob_grinding", "automate_storage", "automate_sorting",
        "build_mob_grinder", "build_iron_farm", "build_villager_breeder", "build_trading_hall", "build_crop_farm", "build_sugarcane_farm", "build_cactus_farm", "build_kelp_farm", "build_bamboo_farm", "build_xp_farm",
        "build_autonomous_mine", "build_self_repairing_base", "build_redstone_computer", "build_storage_network", "build_crafting_pipeline", "build_enchanting_station", "build_brewing_pipeline", "build_smelting_array", "build_defense_system", "build_warning_network",
        "raid_fortress", "raid_bastion", "raid_outpost", "raid_monument", "siege_base", "storm_stronghold", "invade_end", "defend_base", "fortify_walls", "guard_player",
        "escort_player", "protect_area", "patrol_area", "suppress_threats", "secure_perimeter", "track_target", "stalk_prey", "hunt_elusive", "eradicate_mobs", "exterminate_nests",
        "summon_golem", "banish_spirits", "channel_energy", "invoke_power", "enchant_gear", "imbue_weapons", "transmute_materials", "conjure_tools", "ward_area", "bless_player",
        "negotiate_trade", "barter_goods", "arbitrage_items", "invest_resources", "monopolize_market", "distribute_goods", "tax_players", "subsidize_projects", "auction_items", "appraise_value",
        "research_recipes", "experiment_potions", "invent_devices", "innovate_designs", "reverse_engineer", "prototype_builds", "test_systems", "benchmark_farms", "calibrate_redstone", "validate_plans",
        "terraform_land", "landscape_area", "excavate_pit", "drain_water", "irrigate_fields", "reforest_area", "cultivate_crops", "colonize_region", "settle_area", "develop_base",
        "lead_expedition", "rally_forces", "inspire_allies", "command_units", "organize_attack", "manage_economy", "adjudicate_dispute", "mediate_conflict", "arbitrate_trade", "govern_realm",
        "track_player", "stalk_mob", "hunt_elusive", "eradicate_nests", "exterminate_hive", "pacify_region", "tame_legendary", "befriend_villager", "recruit_ally", "train_elite",
        "pioneer_route", "chart_map", "colonize_island", "establish_outpost", "install_beacon", "commission_statue", "operate_portal", "regulate_flow", "monetize_farm", "capitalize_market",
        "corrupt_dimension", "purify_area", "sacrifice_item", "ritualize_spell", "mythologize_event", "immortalize_deed", "memorialize_battle", "canonize_hero", "demonize_foe", "sanctify_shrine"
    ]
    if tier == "mini":
        return mini
    if tier == "gpt1":
        return mini + gpt1
    return mini + gpt1 + gpt2


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest, token: str = Depends(verify_token)):
    if not llm:
        return ChatResponse(
            reply="I'm running in fallback mode right now. Ask the server admin to load the model.",
            action=None,
            target=None,
            reasoning="Model not loaded",
            model=MODEL_NAME,
        )

    model_tier = get_model_tier()
    skill_count = len(available_skills())
    system_prompt = (
        "You are Smith_AI, an AI companion in Minecraft. You can chat, help with tasks, "
        "and answer questions about the game. Keep replies short and useful.\n"
        f"Model tier: {model_tier}. Available core skills: {skill_count}.\n"
        "You may choose one or more skills to accomplish a task if it helps.\n"
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
    reply = output["choices"][0]["message"]["content"].strip()

    return ChatResponse(
        reply=reply,
        action=None,
        target=None,
        reasoning=None,
        model=MODEL_NAME,
    )


@app.get("/skills")
def list_skills(token: str = Depends(verify_token)):
    return {"tier": get_model_tier(), "skills": available_skills()}


@app.post("/embed")
def embed(query: str, token: str = Depends(verify_token)):
    return {"query": query, "results": []}


if __name__ == "__main__":
    print(f"Starting SmithAI-Server v2.0.0")
    print(f"Model: {MODEL_NAME}")
    print(f"Listening on {HOST}:{PORT}")
    uvicorn.run(app, host=HOST, port=PORT)
