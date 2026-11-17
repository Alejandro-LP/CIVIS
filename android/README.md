# Probar CIVIS en Android Studio

Abre esta carpeta android, que contiene settings.gradle.kts. Selecciona un JDK compatible para Gradle, instala SDK 36, sincroniza y ejecuta app.

El proyecto usa Gradle 9.6.0, AGP 9.2.0 y bytecode JVM 17. La compilación verificada se ejecutó con JDK 26.

Desde la carpeta principal inicia el backend con iniciar-laboratorio.ps1. La API utiliza el puerto 8081. En teléfono configura http://IP_DEL_PC:8081, en emulador http://10.0.2.2:8081 y por USB utiliza adb reverse tcp:8081 tcp:8081 con http://127.0.0.1:8081.

En laboratorio el documento 1234567890 y la contraseña Demo123 permiten entrar como administrador ficticio. El README principal y docs/proyecto/02_Manual_de_uso_y_despliegue.md describen registro, correo y base de datos.

Recordar contraseña guarda las credenciales cifradas con AES GCM y una clave de Android Keystore. El cierre de sesión o cambio de clave elimina el contenido recordado. Las credenciales se vinculan a la URL del servidor.

Compila con .\gradlew.bat :app:assembleDebug :app:lintDebug. El APK queda en app/build/outputs/apk/debug/app-debug.apk. El escáner utiliza Google Play Services y requiere ensayo físico con un documento compatible.
