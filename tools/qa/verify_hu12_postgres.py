"""Verificación HU12 contra Spring Boot y PostgreSQL reales; solo datos ficticios."""
import argparse
import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://127.0.0.1:8080")
    parser.add_argument("--output", type=Path, default=Path("evidencias-ci/hu12-postgresql.json"))
    args = parser.parse_args()
    steps = []
    started = datetime.now(timezone.utc).isoformat()
    outcome = "ERROR"

    def request(method, path, data=None, token=None):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = "Bearer " + token
        payload = json.dumps(data).encode() if data is not None else None
        req = urllib.request.Request(args.base + path, method=method, headers=headers, data=payload)
        try:
            with urllib.request.urlopen(req, timeout=12) as response:
                code, body = response.status, response.read()
        except urllib.error.HTTPError as error:
            code, body = error.code, error.read()
        return code, json.loads(body) if body else {}

    def assert_case(code, description, expected, actual, success):
        steps.append({"id": code, "descripcion": description, "esperado": expected,
                      "observado": actual, "resultado": "APROBADO" if success else "FALLIDO"})
        if not success:
            raise AssertionError(f"{code} {description}: {actual!r}")

    try:
        for _ in range(70):
            try:
                http, data = request("GET", "/api/health")
                if http == 200 and data.get("status") == "UP":
                    break
            except (OSError, ValueError):
                pass
            time.sleep(2)
        else:
            raise RuntimeError("La API no inició en PostgreSQL: revisar civis-api-ci.log")

        assert_case("PG-01", "Salud de la API conectada a PostgreSQL", 200, http, http == 200)
        # /api/health can report UP before CommandLineRunner finishes seeding
        # the laboratory account. Retry only during startup; fail definitively
        # if the expected credentials cannot authenticate after the deadline.
        login_deadline = time.monotonic() + 45
        login_attempts = 0
        while True:
            login_attempts += 1
            http, result = request("POST", "/api/auth/login",
                                   {"identification": "1234567890", "password": "Demo" + "123"})
            if http == 200 and isinstance(result, dict) and result.get("token"):
                break
            if http != 401 or time.monotonic() >= login_deadline:
                break
            time.sleep(2)
        assert_case("PG-02", "Autenticación de laboratorio tras inicialización", 200,
                    {"http": http, "intentos": login_attempts},
                    http == 200 and isinstance(result, dict) and bool(result.get("token")))
        token = result["token"]
        http, registrations = request("GET", "/api/admin/citizen-registrations", token=token)
        assert_case("PG-02B", "Auditoría de altas accesible para ADMIN", 200, http, http == 200)
        http, _ = request("GET", "/api/citizens/1000000011/status")
        assert_case("PG-03", "Consulta sin sesión denegada", 401, http, http == 401)

        citizen_id = "9888776611"
        citizen = {"identification": citizen_id, "firstName": "Ensayo",
                   "lastName": "Integración", "birthDate": "1995-01-15", "birthPlace": "Bogotá",
                   "issueDate": "2013-06-17", "issuePlace": "Bogotá", "rh": "+",
                   "bloodGroup": "O", "heightCm": 165, "required": True}
        http, _ = request("POST", "/api/admin/citizens", citizen, token)
        assert_case("PG-04", "Creación de ciudadano en PostgreSQL", 201, http, http == 201)
        http, _ = request("POST", "/api/admin/citizens", citizen, token)
        assert_case("PG-04B", "Duplicado rechazado", 409, http, http == 409)
        http, result = request("GET", "/api/citizens/" + citizen_id + "/status", token=token)
        assert_case("PG-05", "Estado inicial NO_REQUERIDO",
                    {"status": "NO_REQUERIDO"}, result, http == 200 and result == {"status": "NO_REQUERIDO"})
        http, _ = request("PUT", "/api/admin/citizens/" + citizen_id, citizen, token)
        http, result = request("GET", "/api/citizens/" + citizen_id + "/status", token=token)
        assert_case("PG-05B", "Estado REQUERIDO tras edición", {"status":"REQUERIDO"}, result, http == 200 and result == {"status":"REQUERIDO"})
        http, _ = request("PUT", "/api/admin/citizens/" + citizen_id, {**citizen, "required": False}, token)
        assert_case("PG-06", "Cambio de estado en backend web", 200, http, http == 200)
        http, result = request("GET", "/api/citizens/" + citizen_id + "/status", token=token)
        assert_case("PG-07", "Consulta después de actualizar registro",
                    {"status": "NO_REQUERIDO"}, result, http == 200 and result == {"status": "NO_REQUERIDO"})
        http, result = request("GET", "/api/admin/status-history", token=token)
        entries = result.get("content", []) if isinstance(result, dict) else []
        found = any(row.get("citizenId") == citizen_id and row.get("previousRequired") is True
                    and row.get("newRequired") is False for row in entries)
        assert_case("PG-08", "Historial de modificación de estado", True, found, http == 200 and found)
        http, result = request("GET", "/api/admin/audit", token=token)
        entries = result.get("content", []) if isinstance(result, dict) else []
        found = any(row.get("citizenId") == citizen_id and row.get("outcome") == "NO_REQUERIDO"
                    for row in entries)
        assert_case("PG-09", "Auditoría de consulta persistida", True, found, http == 200 and found)
        http, result = request("GET", "/api/citizens/999999999999/status", token=token)
        assert_case("PG-10", "Documento ausente sin información personal",
                    {"status": "SIN_DATOS"}, result, http == 200 and result == {"status": "SIN_DATOS"})
        outcome = "APROBADO"
    except Exception as error:
        steps.append({"id": "ERROR", "descripcion": str(error), "resultado": "FALLIDO"})
        raise
    finally:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps({
            "inicio_utc": started,
            "fin_utc": datetime.now(timezone.utc).isoformat(),
            "entorno": "GitHub Actions: PostgreSQL 17 real, Spring Boot, datos ficticios",
            "resultado": outcome, "casos": steps,
            "limitaciones": ["No es una prueba de interfaz en Android físico",
                            "No se ha validado una fuente judicial externa",
                            "No acredita HTTPS fuera del entorno de laboratorio"]
        }, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"HU12 PostgreSQL: {len(steps)} casos aprobados")


if __name__ == "__main__":
    main()
