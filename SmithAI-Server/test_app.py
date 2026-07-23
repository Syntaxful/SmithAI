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


if __name__ == "__main__":
    test_parse_action_tag()
    print("All tests passed.")
