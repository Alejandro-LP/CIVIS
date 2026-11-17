"""Create a source archive and an auditable deployment bundle without local secrets."""
import hashlib
import shutil
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'entrega'
BUNDLE = OUTPUT / 'CIVIS_Entrega'


def copy(source, target):
    destination = BUNDLE / target
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(ROOT / source, destination)


def main():
    BUNDLE.mkdir(parents=True, exist_ok=True)
    source_zip = OUTPUT / 'CIVIS-fuentes.zip'
    files = subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z'], cwd=ROOT).decode('utf-8').split('\0')
    with zipfile.ZipFile(source_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
        for name in sorted(set(files)):
            if not name:
                continue
            source = ROOT / name
            if source.is_file():
                archive.write(source, Path('CIVIS-fuentes') / name)
    copy('entrega/CIVIS-fuentes.zip', 'fuentes/CIVIS-fuentes.zip')
    copy('backend/target/civis-api-2.0.0.jar', 'aplicacion/backend/target/civis-api-2.0.0.jar')
    copy('android/app/build/outputs/apk/debug/app-debug.apk', 'aplicacion/android/civis-debug.apk')
    for name in ['docker-compose.yml', 'iniciar-servidor.ps1', 'iniciar-laboratorio.ps1', 'probar-api.ps1']:
        copy(name, 'aplicacion/' + name)
    for name in ['01_schema.sql', '02_demo_citizens.sql', 'consultas.sql', 'ciudadanos_ficticios_10.csv']:
        copy('database/' + name, 'aplicacion/database/' + name)
    for file in (OUTPUT / 'documentacion').iterdir():
        if file.suffix in {'.docx', '.pdf', '.pptx'}:
            copy(file.relative_to(ROOT), 'documentacion/' + file.name)
    for file in (ROOT / 'docs/evidencias').iterdir():
        if file.suffix.lower() in {'.png', '.jpg'}:
            copy(file.relative_to(ROOT), 'evidencias/' + file.name)
    for name in ['verificacion.json', 'verificacion_android.json']:
        copy('docs/proyecto/' + name, 'evidencias/' + name)
    copy('entrega/evidencias/reporte-ciudadanos.pdf', 'evidencias/reporte-ciudadanos.pdf')
    instructions = '''# CIVIS Entrega académica completa

Líder del proyecto y líder técnico Jorge Alejandro López Páez.
Equipo David Santiago Guerrero Tolosa, Daniel Felipe Bejarano Perez y Juan Sebastian Romo Sanabria.

## Inicio con los ejecutables incluidos

Requiere JDK 26 y Docker Desktop activo. En PowerShell abre la carpeta aplicacion.
Configura JAVA_HOME y una DB_PASSWORD propia. Conserva esa misma clave al reiniciar una base ya creada.

```powershell
$env:JAVA_HOME='C:\\Program Files\\Java\\jdk-26.0.1'
$env:DB_PASSWORD='<tu contraseña local>'
.\\iniciar-laboratorio.ps1 -OmitirCompilacion
```

Portal http://localhost:8081 y buzón http://localhost:8025.
PostgreSQL se publica en localhost:5433. El script de base se aplica en volúmenes nuevos.
Cuenta ficticia de laboratorio 1234567890 con contraseña Demo123.

Instala aplicacion/android/civis-debug.apk en un Android compatible.
Configura en la app http://IP_DEL_PC:8081 para la misma Wi-Fi o http://10.0.2.2:8081 para emulador.
La conexión por USB puede usar adb reverse tcp:8081 tcp:8081 y http://127.0.0.1:8081.

## Contenido

- documentacion contiene informe, manual, guion del líder y presentación, en formatos editables y PDF.
- fuentes/CIVIS-fuentes.zip contiene código, pruebas, esquemas, documentación y generadores de artefactos.
- aplicacion contiene JAR, APK, scripts de inicio, Docker Compose y SQL.
- evidencias contiene capturas reales, reporte de ejemplo y resultados de verificación.
- SHA256SUMS.txt permite comprobar la integridad de los archivos.

La distribución de roles y el cronograma constituyen una simulación académica.
El padrón institucional utiliza datos ficticios y requiere aceptación del profesor como supuesto del proyecto.
Se aprobaron 27 casos integrales con PostgreSQL 18 y SMTP local. Se verificó login y cifrado en Android.
El ensayo físico de PDF417 permanece pendiente. Docker Compose propone PostgreSQL 17 y Mailpit.
El APK debug está preparado para pruebas académicas. El uso institucional requiere firma, HTTPS y proveedores autorizados.
'''
    (BUNDLE / 'LEEME.md').write_text(instructions, encoding='utf-8')
    checksums = []
    for file in sorted(BUNDLE.rglob('*')):
        if file.is_file() and file.name != 'SHA256SUMS.txt':
            checksums.append(hashlib.sha256(file.read_bytes()).hexdigest() + '  ' + file.relative_to(BUNDLE).as_posix())
    (BUNDLE / 'SHA256SUMS.txt').write_text('\n'.join(checksums) + '\n', encoding='utf-8')
    final_zip = OUTPUT / 'CIVIS_Entrega_Completa.zip'
    with zipfile.ZipFile(final_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
        for file in sorted(BUNDLE.rglob('*')):
            if file.is_file():
                archive.write(file, Path('CIVIS_Entrega') / file.relative_to(BUNDLE))
    with zipfile.ZipFile(final_zip) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('El ZIP no superó la comprobación de integridad')
        forbidden = [name for name in archive.namelist() if any(part in {'.git', 'tmp', '.idea', '.gradle'} for part in Path(name).parts)]
        if forbidden:
            raise RuntimeError('El paquete contiene archivos temporales')
    print(f'Paquete verificado {final_zip.name} {final_zip.stat().st_size:,} bytes')


if __name__ == '__main__':
    main()
