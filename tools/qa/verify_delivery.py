"""Exercise CIVIS against a disposable lab database and the local SMTP fixture."""
import argparse
import email
import email.policy
import io
import json
import re
import secrets
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from pypdf import PdfReader


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://127.0.0.1:8082")
    parser.add_argument("--mail", type=Path, default=Path("tmp/delivery/mail"))
    parser.add_argument("--output", type=Path, default=Path("docs/proyecto/verificacion.json"))
    args = parser.parse_args()
    checks = []

    def check(name, valid):
        if not valid:
            raise AssertionError(name)
        checks.append({"caso": name, "resultado": "APROBADO"})

    def call(method, path, data=None, token=None, expected=200, raw=False):
        headers = {"Content-Type": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        body = json.dumps(data, ensure_ascii=False).encode("utf-8") if data is not None else None
        request = urllib.request.Request(args.base + path, data=body, headers=headers, method=method)
        try:
            response = urllib.request.urlopen(request, timeout=15)
            status, payload = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, payload = error.code, error.read()
        if status != expected:
            raise AssertionError(f"{method} {path}: esperaba {expected}, recibió {status}: {payload[:180]!r}")
        if raw:
            return payload
        return json.loads(payload) if payload else None

    def temporary_password(recipient, existing):
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            for file in args.mail.glob("*.eml"):
                if file.name in existing:
                    continue
                message = email.message_from_bytes(file.read_bytes(), policy=email.policy.default)
                if recipient in str(message["To"]):
                    body = message.get_content()
                    match = re.search(r": ([A-Za-z0-9]{6})(?:[.\s]|$)", body)
                    if match:
                        return match.group(1)
            time.sleep(.1)
        raise AssertionError("El correo de contraseña temporal no llegó al receptor SMTP")

    health = call("GET", "/api/health")
    check("API de laboratorio activa", health["demo"] and health["status"] == "UP")
    check("Portal Spring MVC disponible", b"PORTAL ADMINISTRATIVO" in call("GET", "/", raw=True))
    check("Estilos y JavaScript disponibles", len(call("GET", "/styles.css", raw=True)) > 100 and len(call("GET", "/app.js", raw=True)) > 100)
    call("GET", "/api/admin/citizens", expected=400)
    call("GET", "/api/admin/citizens", token="invalid", expected=401)
    check("API protege datos sin sesión válida", True)
    call("POST", "/api/auth/login", {"identification": "1234567890", "password": "incorrecta"}, expected=401)
    check("Credenciales incorrectas rechazadas", True)
    admin = call("POST", "/api/auth/login", {"identification": "1234567890", "password": "Demo123"})["token"]
    check("Login de administrador", call("GET", "/api/auth/me", token=admin)["role"] == "ADMIN")
    suffix = str(secrets.randbelow(900000) + 100000)
    officer_id, citizen_id = "99" + suffix + "01", "99" + suffix + "02"
    recipient = f"qa{suffix}@civis.test"
    registry = {"identification": officer_id, "force": "Policía", "rank": "Patrullero", "forceId": "QA-" + suffix, "email": recipient, "active": True}
    call("POST", "/api/admin/registry", registry, admin)
    registration = {**registry, "firstName": "Funcionario", "lastName": "De prueba"}
    call("POST", "/api/auth/register", {**registration, "forceId": "NO-COINCIDE"}, expected=403)
    check("Registro rechaza datos institucionales que no coinciden", True)
    existing = {file.name for file in args.mail.glob("*.eml")}
    call("POST", "/api/auth/register", registration, expected=201)
    password = temporary_password(recipient, existing)
    check("Registro entrega correo SMTP real al receptor local", True)
    check("Contraseña temporal de seis caracteres con mayúscula minúscula y número", len(password) == 6 and bool(re.search("[A-Z]", password)) and bool(re.search("[a-z]", password)) and bool(re.search("[0-9]", password)))
    officer = call("POST", "/api/auth/login", {"identification": officer_id, "password": password})["token"]
    check("Login del funcionario recién registrado", call("GET", "/api/auth/me", token=officer)["identification"] == officer_id)
    call("POST", "/api/auth/register", registration, expected=409)
    check("Registro duplicado rechazado", True)
    call("GET", "/api/admin/officers", token=officer, expected=403)
    call("GET", "/api/admin/report.pdf", token=officer, expected=403)
    check("Perfil funcionario sin permisos administrativos", True)
    citizen = {"identification": citizen_id, "firstName": "María", "lastName": "Verificación" * 7, "birthDate": "1993-02-14", "birthPlace": "Bogotá", "issueDate": "2011-03-02", "issuePlace": "Bogotá", "rh": "+", "bloodGroup": "O", "heightCm": 164, "required": True}
    call("POST", "/api/admin/citizens", citizen, admin)
    saved = next(row for row in call("GET", "/api/admin/citizens?required=true", token=admin) if row["identification"] == citizen_id)
    check("Alta ciudadana conserva los once campos solicitados", all(saved[key] == value for key, value in citizen.items()))
    call("POST", "/api/admin/citizens", {**citizen, "issueDate": "1990-01-01"}, admin, expected=400)
    check("Expedición anterior al nacimiento rechazada", True)
    result = call("GET", f"/api/citizens/{citizen_id}/status", token=officer)
    check("HU12: consulta devuelve únicamente el estado requerido", result == {"status": "REQUERIDO"})
    call("POST", "/api/admin/citizens", {**citizen, "required": False}, admin)
    check("Actualización se refleja en consulta móvil REST", call("GET", f"/api/citizens/{citizen_id}/status", token=officer)["status"] == "NO_REQUERIDO")
    check("Documento ausente devuelve SIN_DATOS", call("GET", "/api/citizens/999999999999/status", token=officer)["status"] == "SIN_DATOS")
    pdf = call("GET", "/api/admin/report.pdf?required=false", token=admin, raw=True)
    pdf_text = "".join(page.extract_text() for page in PdfReader(io.BytesIO(pdf)).pages)
    check("PDF filtrado conserva nombres largos y caracteres españoles", "María" in pdf_text and citizen["lastName"] in re.sub(r"\s+", "", pdf_text))
    check("PDF aplica filtro requerido", citizen_id not in "".join(page.extract_text() for page in PdfReader(io.BytesIO(call("GET", "/api/admin/report.pdf?required=true", token=admin, raw=True))).pages))
    output_pdf = Path("entrega/evidencias/reporte-ciudadanos.pdf")
    output_pdf.parent.mkdir(parents=True, exist_ok=True)
    output_pdf.write_bytes(pdf)
    check("Historial del funcionario conserva sus consultas", any(row["citizenId"] == citizen_id for row in call("GET", "/api/history", token=officer)["content"]))
    check("Auditoría administrativa registra al funcionario", any(row["officerId"] == officer_id for row in call("GET", "/api/admin/audit", token=admin)["content"]))
    existing = {file.name for file in args.mail.glob("*.eml")}
    call("POST", "/api/auth/recover", {"identification": officer_id})
    recovered = temporary_password(recipient, existing)
    call("GET", "/api/auth/me", token=officer, expected=401)
    officer = call("POST", "/api/auth/login", {"identification": officer_id, "password": recovered})["token"]
    check("Recuperación por SMTP revoca la sesión anterior", True)
    call("POST", "/api/auth/change-password", {"oldPassword": recovered, "newPassword": "PruebaLocal2026"}, officer)
    call("GET", "/api/auth/me", token=officer, expected=401)
    officer = call("POST", "/api/auth/login", {"identification": officer_id, "password": "PruebaLocal2026"})["token"]
    check("Cambio de contraseña revoca sesiones y admite la nueva clave", True)
    call("PATCH", f"/api/admin/officers/{officer_id}", {"role": "OFFICER", "active": False}, admin)
    call("GET", "/api/auth/me", token=officer, expected=401)
    check("Desactivar funcionario revoca acceso", True)
    call("PATCH", "/api/admin/officers/1234567890", {"role": "OFFICER", "active": False}, admin, expected=409)
    check("Administrador conserva sus propios permisos", True)
    call("DELETE", f"/api/admin/citizens/{citizen_id}", token=admin)
    check("Eliminación de ciudadano", not any(row["identification"] == citizen_id for row in call("GET", "/api/admin/citizens", token=admin)))
    call("POST", "/api/auth/logout", token=admin)
    call("GET", "/api/auth/me", token=admin, expected=401)
    check("Cerrar sesión revoca el token", True)
    result = {"fecha_utc": datetime.now(timezone.utc).isoformat(), "entorno": "PostgreSQL 18 local aislado y SMTP local de captura", "base": args.base, "casos_aprobados": len(checks), "casos": checks, "limites": ["La cámara PDF417 requiere verificación en un teléfono con Google Play Services", "La fuente institucional se representa mediante un padrón académico autorizado"]}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"APROBADOS {len(checks)} casos contra PostgreSQL y SMTP")


if __name__ == "__main__":
    main()
