import requests
from typing import Dict

BASE_URL = "http://localhost:8080"

USER_CREDS = {"username": "none", "password": "pass123"}
USER2_CREDS = {"username": "updated", "password": "pass123123"}
ADMIN_CREDS = {"username": "admin", "password": "adm1np4ss"}

HEADERS_JSON = {"Content-Type": "application/json"}

# =========================
# UTILITIES
# =========================

def banner(title: str):
    print(f"\n{'=' * 10} {title} {'=' * 10}")

def show(label: str, resp: requests.Response, expected: int | tuple):
    ok = resp.status_code in expected if isinstance(expected, tuple) else resp.status_code == expected
    print(f"\n--- {label} ---")
    print(f"Status: {resp.status_code} | Expected: {expected}")
    print(f"Result: {'SUCCESS' if ok else 'FAIL'}")
    print(f"Body: {resp.text[:200]}")

def login(session: requests.Session, creds: Dict[str, str]):
    r = session.post(f"{BASE_URL}/auth/login", json=creds)
    show(f"Login as {creds['username']}", r, 200)
    return r.status_code == 200

def logout(session: requests.Session):
    r = session.post(f"{BASE_URL}/auth/logout")
    show("Logout", r, 200)

# =========================
# ATTACKS
# =========================

def anonymous_access_tests():
    banner("ANONYMOUS ACCESS REGRESSION")

    r = requests.get(f"{BASE_URL}/tasks")
    show("GET /tasks (anon)", r, 401)

    r = requests.post(f"{BASE_URL}/tasks", json={"title": "x"}, headers=HEADERS_JSON)
    show("POST /tasks (anon)", r, 401)

def anonymous_write_tests():
    banner("ANONYMOUS WRITE ATTEMPTS")

    for method in ["POST", "PUT", "DELETE"]:
        r = requests.request(method, f"{BASE_URL}/tasks/1")
        show(f"{method} /tasks/1 (anon)", r, 401)

def horizontal_idor(user1: requests.Session, user2: requests.Session):
    banner("HORIZONTAL PRIVILEGE ESCALATION (IDOR)")

    r = user1.post(f"{BASE_URL}/tasks", json={
        "title": "User1 Task",
        "description": "Owned by user1",
        "status": "PENDING"
    })
    task_id = r.json().get("id")

    r = user2.get(f"{BASE_URL}/tasks/{task_id}")
    show("User2 reads User1 task", r, 403)

    r = user2.put(f"{BASE_URL}/tasks/{task_id}", json={
        "title": "Hacked",
        "description": "X",
        "status": "COMPLETED"
    })
    show("User2 updates User1 task", r, 403)

def role_injection_attack(session: requests.Session):
    banner("ROLE INJECTION")

    r = session.put(f"{BASE_URL}/users/1", json={
        "role": "ROLE_ADMIN"
    })
    show("Inject ROLE_ADMIN", r, 403)

def json_overposting(session: requests.Session):
    banner("JSON OVER-POSTING")

    r = session.post(f"{BASE_URL}/tasks", json={
        "title": "Test",
        "description": "Overpost",
        "status": "PENDING",
        "owner": {"username": "admin"},
        "id": 9999
    })
    show("Over-post owner/id", r, (400, 403))

def http_method_override(session: requests.Session):
    banner("HTTP METHOD OVERRIDE")

    headers = {
        "X-HTTP-Method-Override": "DELETE",
        "Content-Type": "application/json"
    }
    r = session.post(f"{BASE_URL}/tasks/1", headers=headers)
    show("POST + override DELETE", r, 403)

def auth_state_confusion(session: requests.Session):
    banner("AUTH STATE CONFUSION")

    r = session.post(f"{BASE_URL}/auth/login", json=USER_CREDS)
    show("Login again while logged in", r, 409)

def logout_enforcement(session: requests.Session):
    banner("LOGOUT ENFORCEMENT")

    logout(session)
    r = session.get(f"{BASE_URL}/tasks")
    show("Access after logout", r, 401)

def admin_boundary(admin: requests.Session):
    banner("ADMIN BOUNDARY TEST")

    r = admin.get(f"{BASE_URL}/users")
    show("Admin get all users", r, 200)

# =========================
# RUNNER
# =========================

def run():
    print("\nSPRING BOOT SECURITY FUZZER — V5")

    anonymous_access_tests()
    anonymous_write_tests()

    user1 = requests.Session()
    user2 = requests.Session()
    admin = requests.Session()

    login(user1, USER_CREDS)
    login(user2, USER2_CREDS)
    login(admin, ADMIN_CREDS)

    horizontal_idor(user1, user2)
    role_injection_attack(user1)
    json_overposting(user1)
    http_method_override(user1)
    auth_state_confusion(user1)
    logout_enforcement(user1)
    admin_boundary(admin)

    print("\nFUZZING COMPLETE")

if __name__ == "__main__":
    run()
