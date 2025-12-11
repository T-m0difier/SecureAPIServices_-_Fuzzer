import requests
import base64
import json
from typing import Any

BASE_URL = "http://localhost:8080"

# ============================================================
# VALID KNOWN USERS FOR TESTING
# ============================================================
USER = ("none", "pass123")          # normal user
ADMIN = ("admin", "adm1np4ss")      # admin user

# ============================================================
# PAYLOAD SETS
# ============================================================

FUZZ_PAYLOADS = [
    None,
    "",
    " ",
    "a" * 5000,
    0,
    -1,
    999999999999,
    3.14159265,
    True,
    False,
    [],
    {},
    ["unexpected"],
    {"invalid": "structure"},
    "<script>alert(1)</script>",
    "' OR '1'='1",
    "\" OR \"\"=\"",
    "'; DROP TABLE users; --",
    "😀👍🔥😁💀",
]

MALFORMED_JSON_PAYLOADS = [
    "{",
    "}",
    "{title:",
    '{"title": "test",',
    "[1,2,3",
    "null",
    "true false",
    "{bad json}",
    '"unterminated',
]

AUTH_HEADER_PAYLOADS = [
    "",
    " ",
    "Basic",
    "Basic ",
    "Basic ###",
    "Basic " + "A" * 5000,
    "Bearer " + "x" * 200,
    "Basic 😀🔥",
    "Basic " + base64.b64encode(b"bad:format").decode(),
    "Basic " + base64.b64encode(b"admin:wrongpass").decode(),
    "Basic " + base64.b64encode(b"' OR '1'='1:x").decode(),
]

# ============================================================
# SAFE REQUEST WRAPPER
# ============================================================

def safe_request(method: str, url: str, **kwargs) -> Any:
    try:
        resp = requests.request(method, url, timeout=2, **kwargs)
        return resp
    except Exception as e:
        return f"[ERROR] {e}"

def show_response(label: str, response: Any):
    print(f"\n--- {label} ---")

    if isinstance(response, str):
        print(response)
        return

    print("Status:", response.status_code)
    print("Body:", response.text[:200])

    if response.status_code >= 500:
        print(" **CRITICAL: SERVER CRASH / 5xx ERROR DETECTED!** ")

# ============================================================
# AUTH HEADER FUZZING (CRITICAL FOR BASIC AUTH)
# ============================================================

def fuzz_basic_auth_header(endpoint="/tasks"):
    print("\n==============================")
    print("  FUZZING BASIC AUTH HEADER")
    print("==============================")

    for payload in AUTH_HEADER_PAYLOADS:
        headers = {
            "Authorization": payload,
            "Content-Type": "application/json"
        }

        response = safe_request("GET", BASE_URL + endpoint, headers=headers)
        show_response(f"AuthHeader {payload}", response)

# ============================================================
# FUZZ ENDPOINTS
# ============================================================

def fuzz_endpoint(method, endpoint, auth):
    print(f"\n### Fuzzing {method} {endpoint}")

    for payload in FUZZ_PAYLOADS:
        body = {
            "title": payload,
            "description": payload,
            "status": "PENDING",
            "email": payload,
            "age": payload,
        }

        response = safe_request(
            method,
            BASE_URL + endpoint,
            json=body,
            auth=auth,
            headers={"Content-Type": "application/json"}
        )
        show_response(f"BODY {payload}", response)

def fuzz_malformed(endpoint, auth):
    print(f"\n### Fuzzing malformed JSON {endpoint}")

    for raw in MALFORMED_JSON_PAYLOADS:
        headers = {"Content-Type": "application/json"}
        response = safe_request(
            "POST",
            BASE_URL + endpoint,
            data=raw,
            headers=headers,
            auth=auth,
        )
        show_response(f"BADJSON {raw}", response)

def fuzz_headers(endpoint, auth):
    print("\n==============================")
    print("       FUZZING HEADERS")
    print("==============================")

    for payload in FUZZ_PAYLOADS:
        headers = {
            "X-FUZZ": str(payload),
            "Content-Type": "application/json",
            "Accept": str(payload),
        }

        response = safe_request(
            "GET",
            BASE_URL + endpoint,
            headers=headers,
            auth=auth,
        )
        show_response(f"Header {payload}", response)

def fuzz_path(endpoint_base, auth):
    print("\n==============================")
    print("     FUZZING PATH VARIABLES")
    print("==============================")

    for payload in FUZZ_PAYLOADS:
        url = f"{BASE_URL}{endpoint_base}/{payload}"
        response = safe_request("GET", url, auth=auth)
        show_response(f"Path {payload}", response)

def fuzz_delete(endpoint_base, auth):
    print("\n==============================")
    print("      FUZZING DELETE")
    print("==============================")

    for payload in FUZZ_PAYLOADS:
        url = f"{BASE_URL}{endpoint_base}/{payload}"
        response = safe_request("DELETE", url, auth=auth)
        show_response(f"DELETE {payload}", response)

# ============================================================
# RUN EVERYTHING
# ============================================================

def run_fuzzer():
    print("\n========================================")
    print("     SPRING BOOT FUZZER STARTED")
    print("========================================\n")

    # BASIC AUTH HEADER
    fuzz_basic_auth_header("/tasks")

    # AUTH REGISTER
    fuzz_endpoint("POST", "/auth/register", None)
    fuzz_malformed("/auth/register", None)

    # TASKS
    fuzz_endpoint("POST", "/tasks", USER)
    fuzz_endpoint("PUT", "/tasks/1", USER)
    fuzz_malformed("/tasks", USER)
    fuzz_headers("/tasks", USER)
    fuzz_path("/tasks", USER)
    fuzz_delete("/tasks", ADMIN)

    # USERS
    fuzz_endpoint("POST", "/users", ADMIN)
    fuzz_endpoint("PUT", "/users/1", ADMIN)
    fuzz_headers("/users", ADMIN)
    fuzz_path("/users", ADMIN)
    fuzz_delete("/users", ADMIN)

    print("\n========================================")
    print("        FUZZING COMPLETE")
    print("========================================\n")

# ============================================================

if __name__ == "__main__":
    run_fuzzer()
