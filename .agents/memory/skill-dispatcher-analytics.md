---
name: SkillDispatcher analytics pattern
description: recordSkillUsage tracks per-skill use/success and feeds RL Recorder
---
**Pattern:** `recordSkillUsage(skillName, success, npc, contextPlayer)` is called after every successful skill dispatch in `SkillDispatcher.execute()`. It maintains an in-memory `Map<String, int[]>` (use count, success count) and writes to `RLDataRecorder.record("skill_usage", skillName, score)`.

**Why:** Provides both real-time analytics (`/smithai data` can expose `getSkillUsage()`) and persistent RL training feedback.

**How to apply:** When adding new skill dispatch paths in `execute()`, add `recordSkillUsage(skillName, true, npc, contextPlayer);` before the return. For failure-only cases, pass `false`.
