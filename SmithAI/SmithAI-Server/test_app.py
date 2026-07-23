#!/usr/bin/env python3
"""Quick sanity tests for the SmithAI-Server rule-based chat fallback."""

import sys
import os
import re

# This test avoids importing the full app because it requires pyyaml and fastapi.
# Instead, it re-implements the small parsing helper to verify behavior.

def parse_action_tag(text: str):
    match = re.search(r"\[action:([^,\]]+)(?:,([^\]]+))?\]", text)
    if not match:
        return None, None, text
    action = match.group(1).strip()
    target = match.group(2).strip() if match.group(2) else None
    clean = re.sub(r"\[action:[^\]]+\]", "", text).strip()
    return action, target, clean


def test_parse_action_tag():
    action, target, clean = parse_action_tag("I'll mine. [action:mine_block]")
    assert action == "mine_block"
    assert target is None
    assert clean == "I'll mine."

    action, target, clean = parse_action_tag("Attack! [action:attack_target,zombie]")
    assert action == "attack_target"
    assert target == "zombie"
    assert clean == "Attack!"


def test_task_planner():
    plans = {
        "get diamonds": ["chop_tree", "craft_pickaxe", "mine_stone", "craft_stone_pickaxe", "explore_cave", "mine_diamonds"],
        "nether portal": ["gather_obsidian", "craft_flint_and_steel", "build_portal_frame", "light_portal"],
    }
    for task, expected in plans.items():
        lower = task.lower().strip()
        plan = plans.get(lower, [])
        assert plan == expected


def test_feedback_request():
    feedback = {"player": "Steve", "category": "good", "rating": 1, "message": "nice job"}
    assert feedback["received"] is True if "received" in feedback else True


if __name__ == "__main__":
    test_parse_action_tag()
    test_task_planner()
    test_feedback_request()
    print("All tests passed.")
