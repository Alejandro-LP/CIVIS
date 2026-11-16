# Probar CIVIS en Android Studio

Abre `CIVIS_MINIMAL_MILITAR_COMPLETO/android`, que contiene `settings.gradle.kts`.
Abrir `CIVIS_V3` importa un proyecto genérico y no configura la aplicación Android.
También puedes ejecutar `abrir-android-studio.cmd` desde la carpeta principal.

1. Selecciona File > Open y abre la carpeta `android` indicada.
2. Espera a que termine Sync Project with Gradle Files.
3. En Settings > Build, Execution, Deployment > Build Tools > Gradle, configura
   Gradle JDK. Para reproducir el entorno de esta entrega, selecciona el JDK
   instalado en `C:\Program Files\Java\jdk-26.0.1` si existe en tu equipo.
4. Comprueba en SDK Manager que esté instalada Android SDK Platform 36.
5. Selecciona `app` y un emulador o teléfono con Android 8 (API 26) o superior.
   Pulsa Run.

El proyecto configura Gradle 9.4.1, AGP 9.2.0 y bytecode JVM 17.

## Servidor para iniciar sesión

Desde `CIVIS_MINIMAL_MILITAR_COMPLETO`, con Docker disponible:

```powershell
docker compose up -d
.\iniciar-servidor.ps1 -Laboratorio
```

En «Configurar servidor API» usa `http://10.0.2.2:8080` para el emulador.
En un teléfono usa `http://IP_DEL_PC:8080`, con ambos equipos en la misma red.
En laboratorio: documento `1234567890`, contraseña `Demo123`.
Antes de iniciar Docker y el backend, configura la misma variable `DB_PASSWORD`
con tu contraseña local de PostgreSQL en ambas terminales.
Documentos de prueba: `1000000011` a `1000000020`.

Si no aparece `app`, verifica la carpeta abierta. Si falla Sync, copia el primer
error de Build Output. Si falla el acceso dentro de la app, comprueba el servidor
y la URL configurada.

Para compilar desde esta carpeta:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
Consulta el README principal para configurar el servidor.
