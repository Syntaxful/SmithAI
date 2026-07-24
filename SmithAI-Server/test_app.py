#!/usr/bin/env python3
"""Quick sanity test for the SmithAI-Server FastAPI app."""

import os
import sys

# Ensure app.py can load the local config
os.environ.setdefault("SMITHAI_CONFIG", os.path.join(os.path.dirname(__file__), "config.yml"))

from fastapi.testclient import TestClient
from app import app, parse_action_tag, TASK_PLANS

client = TestClient(app)


def test_parse_action_tag():
    assert parse_action_tag("I will follow you. [action:follow_player]") == ("follow_player", None, "I will follow you.")
    assert parse_action_tag("Mining. [action:mine_block,diamond_ore]") == ("mine_block", "diamond_ore", "Mining.")
    assert parse_action_tag("No action here") == (None, None, "No action here")


def test_health_no_auth():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "model" in data
    assert "tier" in data


def test_root_no_auth():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"


def test_task_plans():
    assert "get diamonds" in TASK_PLANS
    assert "beat the game" in TASK_PLANS


if __name__ == "__main__":
    test_parse_action_tag()
    test_health_no_auth()
    test_root_no_auth()
    test_task_plans()
    print("All tests passed.")
