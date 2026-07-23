# SmithAI-Server API Reference

The SmithAI-Server is a Python FastAPI application that provides an external AI brain for the SmithAI Minecraft plugin. It runs on the user's own host.

## Base URL

The server runs at `http://HOST:PORT` as configured in `config.yml` or by the `PORT` environment variable.

## Authentication

All endpoints except `/health` require a Bearer token in the `Authorization` header. The server generates a key starting with `SMA-` on first startup and prints it to the console.

```bash
curl -H "Authorization: Bearer SMA-xxxxxxxx" http://localhost:8000/chat
```

## Endpoints

### GET /health

Public health check. Returns model status and loaded tier.

**Response:**
```json
{
  "status": "ok",
  "model": "SmithGPT 1.0 7.5GB",
  "tier": "gpt1",
  "skills": 40,
  "model_loaded": true
}
```

### POST /chat

Main chat endpoint. Accepts conversation messages and optional context.

**Request body:**
```json
{
  "messages": [
    { "role": "user", "content": "Smith_AI, find diamonds" }
  ],
  "model": "smithgpt-1.0",
  "task": "get diamonds",
  "knowledge": ["Diamond ore is found deep underground..."],
  "skills": ["mine_block", "gather_diamonds"],
  "context": { "player": "Steve", "world": "world" }
}
```

**Response:**
```json
{
  "reply": "I'll mine for diamonds at Y=-59.",
  "action": "mine_block",
  "target": null,
  "reasoning": "Rule-based fallback",
  "model": "SmithGPT 1.0 7.5GB"
}
```

The server may return an `[action:skill_name]` or `[action:skill_name,target]` tag inside the reply. The plugin parses this and queues the action.

### GET /skills

Returns the list of skills available for the active model tier.

**Response:**
```json
{
  "tier": "gpt1",
  "skills": ["follow_player", "mine_block", ...]
}
```

### POST /task

Returns a suggested step plan for a given task.

**Request body:**
```json
{
  "task": "get diamonds",
  "context": {}
}
```

**Response:**
```json
{
  "task": "get diamonds",
  "plan": ["chop_tree", "craft_pickaxe", "mine_stone", "craft_stone_pickaxe", "explore_cave", "mine_diamonds"],
  "model": "SmithGPT 1.0 7.5GB"
}
```

Known tasks include: `get diamonds`, `nether portal`, `build base`, `beat the game`, `fight`, `get food`, `farm`.

### POST /feedback

Receives training feedback from the plugin.

**Request body:**
```json
{
  "player": "Steve",
  "message": "you mined the wrong block",
  "category": "general",
  "rating": -1,
  "context": { "task": "get diamonds" }
}
```

**Response:**
```json
{
  "received": true,
  "player": "Steve",
  "category": "general",
  "rating": -1,
  "message": "you mined the wrong block",
  "model": "SmithGPT 1.0 7.5GB"
}
```

### POST /embed

Stub endpoint for knowledge embedding/retrieval. Currently returns the query and an empty results list.

**Request body:**
```json
{ "query": "how to find diamonds" }
```

**Response:**
```json
{
  "query": "how to find diamonds",
  "results": []
}
```

Future versions will return relevant knowledge entries from the SmithAI knowledge base.

## Error responses

- `401 Unauthorized`: Missing or invalid `Authorization` header.
- `422 Unprocessable Entity`: Invalid request body.
- `500 Internal Server Error`: LLM or server error. The plugin falls back to Smith-Mini 1.0.

## Configuration

See `SmithAI-Server/config.yml` for server settings:

```yaml
server:
  host: "0.0.0.0"
  port: 8000
model:
  path: "models/smithgpt-1.0-7.5.gguf"
  name: "SmithGPT 1.0 7.5GB"
  context_size: 4096
  max_tokens: 200
  n_threads: 4
security:
  api_key: ""
```

The `api_key` is auto-generated on first run if left empty. Set `PORT` as an environment variable to override `server.port` (useful on Replit and Codespaces).

## Docker

The server can be run with Docker:

```bash
docker build -t smithai-server SmithAI-Server
docker run -p 8000:8000 -v $(pwd)/models:/models smithai-server
```

Or use `docker-compose.yml` at the repository root.
