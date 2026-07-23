#!/usr/bin/env python3
"""Integration test for SmithAI-Server API endpoints.
Tests that the server can start, serve requests, and respond correctly.

Usage: python3 integration_test.py [http://localhost:8000]

Requires: requests, pyyaml (pip install requests pyyaml)
"""

import sys
import os
import json
import subprocess
import time
import signal

API_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8000"
API_KEY = os.environ.get("SMITHAI_API_KEY", "test-key")
TOTAL_TESTS = 0
FAILED_TESTS = 0


def test(name, condition, detail=""):
    global TOTAL_TESTS, FAILED_TESTS
    TOTAL_TESTS += 1
    if condition:
        print(f"  ✓ {name}")
    else:
        FAILED_TESTS += 1
        print(f"  ✗ {name} — {detail}")


def check(url, auth=True):
    """HTTP GET helper with optional auth."""
    try:
        import requests
        headers = {"Authorization": f"Bearer {API_KEY}"} if auth else {}
        resp = requests.get(url, headers=headers, timeout=5)
        return resp
    except Exception as e:
        return None


if __name__ == "__main__":
    print("\nSmithAI-Server Integration Tests")
    print("=" * 40)

    # 1. Health endpoint (no auth)
    print("\n1. Health endpoint:")
    h = check(f"{API_URL}/health", auth=False)
    test("returns 200", h is not None and h.status_code == 200,
         f"got {h.status_code if h else 'connection failed'}")
    if h:
        data = h.json()
        test("returns json with status", "status" in data,
             f"keys: {list(data.keys())}")
        test("returns model name", "model" in data and data["model"],
             f"model: {data.get('model')}")

    # 2. Status dashboard (no auth)
    print("\n2. Status dashboard:")
    s = check(f"{API_URL}/status", auth=False)
    test("returns 200", s is not None and s.status_code == 200,
         f"got {s.status_code if s else 'connection failed'}")
    test("returns HTML", s is not None and "text/html" in s.headers.get("content-type", ""),
         f"content-type: {s.headers.get('content-type', 'N/A') if s else 'N/A'}")

    # 3. Skills endpoint (requires auth)
    print("\n3. Skills endpoint:")
    sk = check(f"{API_URL}/skills", auth=True)
    test("returns 200", sk is not None and sk.status_code == 200,
         f"got {sk.status_code if sk else 'connection failed'}")
    if sk and sk.status_code == 200:
        data = sk.json()
        test("returns tier and skills", "tier" in data and "skills" in data,
             f"keys: {list(data.keys())}")
        test("skills is a list", isinstance(data.get("skills"), list),
             f"type: {type(data.get('skills'))}")

    # 4. Auth rejection
    print("\n4. Auth rejection:")
    sk_noauth = check(f"{API_URL}/skills", auth=False)
    test("rejects without auth", sk_noauth is not None and sk_noauth.status_code == 401,
         f"got {sk_noauth.status_code if sk_noauth else 'connection failed'}")

    # 5. RL data endpoint (no auth)
    print("\n5. RL data endpoint:")
    rl = check(f"{API_URL}/rl_data", auth=False)
    test("returns 200", rl is not None and rl.status_code == 200,
         f"got {rl.status_code if rl else 'connection failed'}")
    if rl:
        data = rl.json()
        test("has status field", data.get("status") == "ok",
             f"status: {data.get('status')}")

    # 6. Task planner
    print("\n6. Task planner:")
    if sk and sk.status_code == 200:
        try:
            import requests
            resp = requests.post(f"{API_URL}/task",
                                 json={"task": "get diamonds", "context": {}},
                                 headers={"Authorization": f"Bearer {API_KEY}"},
                                 timeout=5)
            test("returns 200", resp.status_code == 200,
                 f"got {resp.status_code}")
            data = resp.json()
            test("returns task plan", "plan" in data and len(data.get("plan", [])) > 0,
                 f"plan: {data.get('plan')}")
        except Exception as e:
            test("POST /task works", False, str(e))

    # Summary
    print(f"\n{'=' * 40}")
    print(f"Results: {TOTAL_TESTS - FAILED_TESTS}/{TOTAL_TESTS} passed")
    if FAILED_TESTS > 0:
        print(f"FAILURES: {FAILED_TESTS} test(s) failed")
        sys.exit(1)
    else:
        print("All integration tests passed!")
        sys.exit(0)
