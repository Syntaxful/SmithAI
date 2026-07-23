# SmithAI FAQ

## Does SmithAI work with Eaglercraft?

Yes. The plugin is built for Bukkit/Spigot/Paper 1.21.x, which is the server-side API used by Eaglercraft servers.

## Do players need an account to use SmithAI?

No. Players just talk to `Smith_AI` in chat. Only the server admin needs to set up the API key if using SmithGPT 1.0 or 2.0.

## What is the difference between Smith-Mini, SmithGPT 1.0, and SmithGPT 2.0?

- **Smith-Mini 1.0** — small model that runs inside the Minecraft server. No setup needed.
- **SmithGPT 1.0** — 4GB external model. Requires SmithAI-Server hosted elsewhere.
- **SmithGPT 2.0** — 7.5GB external model. Requires more RAM but gives better replies.

## Can I run SmithAI without any external hosting?

Yes. Smith-Mini 1.0 works entirely inside the Minecraft server.

## Where does the API key come from?

SmithAI-Server generates it on first start and prints it to the console. The key starts with `SMA-`. You paste it into the Minecraft server with `/SmithAPI set SMA-...`.

## What if the external server goes offline?

The plugin automatically switches back to Smith-Mini 1.0 and tells players. It keeps trying to reconnect.

## How do I train the AI?

Say `good bot` or `bad bot` near `Smith_AI`, or use `/smithai train good` and `/smithai train bad`. Training data is saved to disk.

## How do I tell the AI exactly what it did wrong?

Use `/smithai feedback <what it did wrong>`, for example `/smithai feedback you broke the wrong block`. The AI stores this feedback and tries to avoid it. You can also say things like `don't do that` or `that was wrong` in chat.

## How do I report a bug or request a feature?

Use `/smithai report <short description>` in-game. The plugin will give you a link to open a GitHub issue with details already filled in. If the link is too long for chat, it also saves the report to `plugins/SmithAI/issue_reports.yml`.

## Can Smith_AI really beat the game?

Beating the game is a long-term goal. The plugin includes a task planner that knows the sequence of steps, but executing every step in a dynamic world is complex. The system is designed to be expanded skill by skill.

## How do I add more skills?

Edit the `skills.yml` file in the plugin data folder, or add new skill definitions to the source code and rebuild the plugin.

## Where are the build and release scripts?

- `build.sh` — builds the plugin JAR for non-Maven users
- `package-release.sh` — bundles the plugin JAR, server, docs, and checksums into a zip
- `.github/workflows/build.yml` — CI that builds the plugin and checks the server on every push
