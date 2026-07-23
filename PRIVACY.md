# SmithAI Privacy Policy

Last updated: July 23, 2026

SmithAI is free forever and does not collect player data.

## What we collect

**Nothing.** SmithAI does not send player data, chat logs, or server telemetry to any centralized service. The plugin only stores data locally on the Minecraft server:

- `plugins/SmithAI/memory.yml` — per-player conversation history, used by the NPC
- `plugins/SmithAI/training_data.yml` — feedback scores (e.g., `good bot` / `bad bot`)
- `plugins/SmithAI/feedback_data.yml` — written feedback about what the AI did wrong
- `plugins/SmithAI/issue_reports.yml` — bug reports and feature requests submitted in-game
- `plugins/SmithAI/skills.yml` — generated skill definitions
- `plugins/SmithAI/config.yml` — local plugin configuration

## External AI (optional)

If you enable the external SmithAI-Server, chat messages are sent to the server **you** host. The traffic does not pass through any third-party service controlled by the SmithAI authors. The server URL, API key, and model are entirely under your control.

## API keys

The SmithAI-Server generates an API key locally on first start. It is only printed to the server console. Keep it secret and share it only with server administrators who need to configure the plugin.

## Open source

SmithAI is open source. You can inspect the code to verify that no data is collected or sent anywhere without your explicit setup of an external server.

## Changes to this policy

If this policy ever changes, the updated version will be included in the repository and release notes.
