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
        http, result = request("POST", "/api/auth/login",
                               {"identification": "1234567890", "password": "Demo123"})
        assert_case("PG-02", "Autenticación de laboratorio", 200, http, http == 200 and "token" in result)
        token = result["token"]
        http, _ = request("GET", "/api/citizens/1000000011/status")
        assert_case("PG-03", "Consulta sin sesión denegada", 401, http, http == 401)

        citizen_id = "9888776611"
        citizen = {"identification": citizen_id, "firstName": "Ensayo",
                   "lastName": "Integración", "required": True}
        http, _ = request("POST", "/api/admin/citizens", citizen, token)
        assert_case("PG-04", "Creación de ciudadano en PostgreSQL", 200, http, http == 200)
        http, result = request("GET", "/api/citizens/" + citizen_id + "/status", token=token)
        assert_case("PG-05", "Privacidad: solo status REQUERIDO",
                    {"status": "REQUERIDO"}, result, http == 200 and result == {"status": "REQUERIDO"})
        http, _ = request("POST", "/api/admin/citizens", {**citizen, "required": False}, token)
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
