import requests


BASE_URL = "http://localhost:8080/"

# ---------------------------
# Payloads for fuzzing
# ---------------------------
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

# ---------------------------
# Helper: Safe request wrapper
# ---------------------------
def safe_request(method, url, **kwargs):
    try:
        response = requests.request(method, url, timeout=3, **kwargs)
        return response
    except Exception as e:
        return f"ERROR: {e}"

# ---------------------------
# Fuzz POST/PUT with payloads
# ---------------------------
def fuzz_body(method, endpoint):
    print(f"\n[+] Fuzzing BODY for {method} {endpoint}")
    
    for payload in FUZZ_PAYLOADS:
        data = {
            "title": payload,
            "description": payload,
            "status": payload,
            "name": payload,
            "email": payload,
            "age": payload
        }

        try:
            response = safe_request(method, BASE_URL + endpoint, json=data)
        except:
            response = None

        print(f"\nPayload: {payload}")
        print("Response:", response)

        if hasattr(response, "status_code") and response.status_code >= 500:
            print("**Critical: Server crashed with 5xx!**")

# ---------------------------
# Fuzz PATH parameters
# ---------------------------
def fuzz_ids(endpoint_base):
    print(f"\n[+] Fuzzing PATH IDs for {endpoint_base}/<id>")

    for payload in FUZZ_PAYLOADS:
        url = f"{BASE_URL}{endpoint_base}/{payload}"

        response = safe_request("GET", url)

        print(f"\nPath ID: {payload}")
        print("Response:", response)

        if hasattr(response, "status_code") and response.status_code >= 500:
            print("**Critical: Server crashed with 5xx!**")

# ---------------------------
# Fuzz DELETE endpoints
# ---------------------------
def fuzz_delete(endpoint_base):
    print(f"\n[+] Fuzzing DELETE for {endpoint_base}/<id>")

    for payload in FUZZ_PAYLOADS:
        url = f"{BASE_URL}{endpoint_base}/{payload}"

        response = safe_request("DELETE", url)
        
        print(f"\nDELETE ID: {payload}")
        print("Response:", response)

        if hasattr(response, "status_code") and response.status_code >= 500:
            print("**Critical: Server crashed with 5xx!**")

# ---------------------------
# Run all fuzz tests
# ---------------------------
def run_fuzzer():
    print("\n========================================")
    print("       Web Service Fuzzer Started")
    print("========================================")

    # --- USERS ---
    fuzz_body("POST", "/users")
    fuzz_body("PUT", "/users/1")
    fuzz_ids("/users")
    fuzz_delete("/users")

    # --- TASKS ---
    fuzz_body("POST", "/tasks")
    fuzz_body("PUT", "/tasks/1")
    fuzz_ids("/tasks")
    fuzz_delete("/tasks")

    print("\n========================================")
    print("        🏁 Fuzzing Complete")
    print("========================================")

# -----------------------------------------
# Main
# -----------------------------------------
if __name__ == "__main__":
    run_fuzzer()