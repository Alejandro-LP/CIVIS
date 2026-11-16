package com.civis.mobile

import android.os.Bundle
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

private val Olive = Color(0xFF4B5D3A)
private val OliveDeep = Color(0xFF34452B)
private val OlivePale = Color(0xFFE8EEE0)
private val Canvas = Color(0xFFF8F9F5)
private val Ink = Color(0xFF20271D)
private val Muted = Color(0xFF667063)
private val Line = Color(0xFFDDE4D7)
private val Success = Color(0xFF277345)
private val Danger = Color(0xFFA73737)
private val LocalDemo = compositionLocalOf { true }

private val rankMap = mapOf(
    "Policía" to listOf("Patrullero", "Subintendente", "Intendente", "Subcomisario", "Comisario", "Subteniente", "Teniente", "Capitán", "Mayor", "Teniente coronel", "Coronel", "General"),
    "Ejército" to listOf("Soldado", "Cabo", "Sargento", "Subteniente", "Teniente", "Capitán", "Mayor", "Teniente coronel", "Coronel", "General"),
    "Armada" to listOf("Marinero", "Cabo", "Suboficial", "Teniente de corbeta", "Teniente de fragata", "Teniente de navío", "Capitán de corbeta", "Capitán de fragata", "Capitán de navío", "Almirante"),
    "Fuerza Aérea" to listOf("Técnico", "Suboficial", "Subteniente", "Teniente", "Capitán", "Mayor", "Teniente coronel", "Coronel", "General")
)

internal data class Officer(val id: String, val first: String, val last: String, val force: String, val rank: String, val forceId: String, val email: String, val role: String = "OFFICER")
private sealed interface Lookup { data object Missing : Lookup; data class Result(val required: Boolean, val id: String, val name: String) : Lookup }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("civis_local", MODE_PRIVATE)
        val api = CivisApi()
        setContent {
            var demoMode by remember { mutableStateOf(true) }
            CivisTheme(demoMode) {
                val scope = rememberCoroutineScope()
                var page by remember { mutableStateOf("login") }
                var serverUrl by remember { mutableStateOf(preferences.getString("server_url", "http://192.168.1.100:8080").orEmpty()) }
                api.baseUrl = serverUrl
                var currentUser by remember { mutableStateOf<Officer?>(null) }
                var token by remember { mutableStateOf("") }
                var enteredId by remember { mutableStateOf("") }
                var pendingResult by remember { mutableStateOf<Lookup?>(null) }
                var scannedValue by remember { mutableStateOf("") }
                var scannerError by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var alert by remember { mutableStateOf("") }
                var alertIsError by remember { mutableStateOf(true) }
                fun performLookup(value: String) {
                    if(busy) return
                    val lookupId=value.filter(Char::isDigit)
                    enteredId = lookupId
                    busy = true; alert = ""
                    scope.launch {
                        try {
                            val outcome = withContext(Dispatchers.IO) { api.lookup(token, lookupId) }
                            pendingResult = when(outcome.status) {
                                "REQUERIDO" -> Lookup.Result(true,enteredId,outcome.name)
                                "NO_REQUERIDO" -> Lookup.Result(false,enteredId,outcome.name)
                                else -> Lookup.Missing
                            }
                            page = "result"
                        } catch(e: Exception) { alert = e.message ?: "No se pudo consultar el servidor" }
                        busy = false
                    }
                }
                BackHandler(enabled = page != "login") {
                    if(page=="home") { moveTaskToBack(true);return@BackHandler }
                    page = when (page) {
                        "result" -> "consult"
                        "consult", "profile", "manage", "history", "password" -> "home"
                        "register", "recover" -> "login"
                        else -> "login"
                    }
                }
                when (page) {
                    "login" -> LoginScreen(preferences.getString("remembered", "").orEmpty(), onLogin = { id, pass, remember ->
                        alertIsError = true
                        busy = true; alert = ""
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) { api.login(id, pass) }
                                token = response.first; currentUser = response.second;demoMode=api.demoMode
                                preferences.edit().putString("remembered", if(remember) id else "").apply()
                                page = "home"
                            } catch(e: Exception) { alert = e.message ?: "Error de autenticación" }
                            busy = false
                        }
                    }, onRegister = { alert = ""; page = "register" }, onRecover = { alert = ""; page = "recover" }, busy = busy, networkError = alert, messageIsError = alertIsError, serverUrl = serverUrl, onServerUrl = { serverUrl = it; api.baseUrl = it; preferences.edit().putString("server_url",it).apply() }, onCheckServer = {
                        busy = true
                        alert = ""
                        alertIsError = true
                        scope.launch {
                            try {
                                val laboratory = withContext(Dispatchers.IO) { api.checkConnection() }
                                alertIsError = false
                                alert = if (laboratory) "Conexión correcta. Laboratorio activo. Puedes usar los datos de prueba indicados."
                                    else "Conexión correcta. Usa una cuenta autorizada por tu administrador."
                            } catch (e: Exception) {
                                alert = e.message ?: "No se pudo conectar con CIVIS."
                            }
                            busy = false
                        }
                    })
                    "register" -> RegisterScreen(onBack = { page = "login" }, onRegister = { officer ->
                        busy = true; alert = ""
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { api.register(officer) }
                                alert = "Registro exitoso: revisa tu correo para obtener la contraseña temporal."
                                alertIsError = false
                                page = "login"
                            } catch(e: Exception) { alert = e.message ?: "Error de registro" }
                            busy = false
                        }
                    }, busy = busy, networkError = alert)
                    "recover" -> RecoverScreen(onBack = { page = "login" }, onRecover = { id ->
                        busy = true; alert = ""
                        scope.launch {
                            try { withContext(Dispatchers.IO) { api.recover(id) }; alert = "Si existe una cuenta, recibirás un correo." }
                            catch(e: Exception){ alert = e.message ?: "Error al recuperar" }
                            busy = false
                        }
                    }, busy = busy, message = alert)
                    "manage" -> ManagementScreen(api,token,currentUser!!.id,onBack={page="home"})
                    "history" -> HistoryScreen(api,token,onBack={page="home"})
                    "password" -> PasswordScreen(api,token,onBack={page="home"},onChanged={token="";currentUser=null;alert="Contraseña actualizada. Inicia sesión con tu nueva contraseña.";page="login"})
                    "home" -> HomeScreen(currentUser!!, onScan = {
                        scannerError = ""
                        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_PDF417, Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128).enableAutoZoom().build()
                        GmsBarcodeScanning.getClient(this, options).startScan()
                            .addOnSuccessListener { barcode -> scannedValue = barcode.rawValue.orEmpty(); page = "consult" }
                            .addOnFailureListener { scannerError = "Escáner no disponible. Usa la consulta manual." }
                    }, onConsult = { scannedValue = ""; alert = ""; page = "consult" }, onProfile = { page = "profile" }, scannerError = scannerError,
                    onManage={page="manage"},onHistory={page="history"},onPassword={page="password"})
                    "consult" -> { Column {
                        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if(alert.isNotBlank()) Hint(alert,true)
                        ConsultScreen(scannedValue, onBack = {page="home"}, onSubmit = ::performLookup)
                    } }
                    "result" -> ResultScreen(enteredId, pendingResult ?: Lookup.Missing, onBack = { page = "consult" }, onNew = { scannedValue = ""; page = "consult" })
                    "profile" -> ProfileScreen(currentUser!!, onBack = { page = "home" }, onExit = {
                        val logoutToken=token; scope.launch { try { withContext(Dispatchers.IO){ api.logout(logoutToken) } } catch(_:Exception) {} }
                        token=""; currentUser=null; page="login"
                    })
                }
            }
        }
    }
}

@Composable
private fun CivisTheme(currentDemoMode:Boolean,content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDemo provides currentDemoMode) { MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Olive, onPrimary = Color.White,
            secondary = OliveDeep, background = Canvas,
            surface = Color.White, onSurface = Ink,
            onBackground = Ink, outline = Line,
            error = Danger
        ), content = content
    ) }
}

@Composable
private fun Screen(
    title: String, subtitle: String,
    back: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Canvas)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Ink)
                }
            } else {
                Brand()
            }
            Spacer(Modifier.weight(1f))
            Surface(color = OlivePale, shape = RoundedCornerShape(50.dp)) {
                Text(if(LocalDemo.current) "DEMO" else "REGISTRO INTERNO", Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    fontSize = 10.sp, letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold, color = OliveDeep)
            }
        }
        Spacer(Modifier.height(30.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 29.sp, color = Ink,
            lineHeight = 35.sp)
        Spacer(Modifier.height(7.dp))
        Text(subtitle, color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(26.dp))
        content()
        Spacer(Modifier.height(34.dp))
        Text(if(LocalDemo.current) "CIVIS  /  ENTORNO ACADÉMICO DE PRUEBA" else "CIVIS / GESTIÓN Y CONSULTA INTERNA", color = Muted,
            fontSize = 10.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(13.dp), color = Olive) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Color.White,
                modifier = Modifier.padding(10.dp).size(23.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("civis.", fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp, color = Ink, letterSpacing = (-0.5).sp)
            Text("CONSULTA CIUDADANA", fontWeight = FontWeight.Medium,
                fontSize = 9.sp, color = Muted, letterSpacing = 0.8.sp)
        }
    }
}

@Composable private fun MinimalCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable private fun Field(
    label: String, value: String, onValue: (String) -> Unit,
    hint: String = "", password: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text
) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    Spacer(Modifier.height(7.dp))
    OutlinedTextField(
        value = value, onValueChange = onValue, modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint, fontSize = 13.sp, color = Muted) },
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation()
            else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboard),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Line, focusedBorderColor = Olive,
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            cursorColor = Olive
        )
    )
    Spacer(Modifier.height(17.dp))
}

@Composable private fun MainButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(53.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Olive, contentColor = Color.White)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
    }
}

@Composable private fun Hint(text: String, error: Boolean = false) {
    val bg = if (error) Color(0xFFF9EBE9) else OlivePale
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(if (error) Icons.Rounded.ErrorOutline else Icons.Rounded.Info,
                contentDescription = null, tint = if (error) Danger else OliveDeep,
                modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(9.dp))
            Text(text, color = if (error) Danger else OliveDeep,
                fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
    Spacer(Modifier.height(13.dp))
}

@Composable private fun LoginScreen(
    saved: String, onLogin: (String, String, Boolean) -> Unit,
    onRegister: () -> Unit, onRecover: () -> Unit, busy: Boolean,
    networkError: String, messageIsError: Boolean, serverUrl: String, onServerUrl: (String)->Unit,
    onCheckServer: () -> Unit
) {
    var id by remember(saved) { mutableStateOf(saved) }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(saved.isNotBlank()) }
    var error by remember { mutableStateOf("") }
    var showServer by remember { mutableStateOf(false) }
    Screen("Bienvenido a CIVIS", "Acceso exclusivo para personal de la fuerza pública.") {
        MinimalCard {
            Text("Iniciar sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(5.dp))
            Text("Ingresa tus credenciales institucionales", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(23.dp))
            Field("Número de identificación", id,
                { id = it.filter(Char::isDigit).take(12); error = "" },
                "Ej. 1234567890", keyboard = KeyboardType.Number)
            Field("Contraseña", password, { password = it; error = "" },
                "Contraseña recibida por correo", password = true)
            Text("Usa tu documento sin puntos ni espacios y la contraseña de tu cuenta.", color = Muted, fontSize = 12.sp)
            if ((androidx.compose.ui.platform.LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                Spacer(Modifier.height(10.dp))
                Hint("Ejemplo de laboratorio\nIdentificación 1234567890\nContraseña Demo123\nDisponible cuando el servidor está iniciado en modo laboratorio.")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = remember, onCheckedChange = {remember = it},
                    colors = CheckboxDefaults.colors(checkedColor = Olive))
                Text("Recordar identificación", fontSize = 12.sp, color = Ink)
            }
            Spacer(Modifier.height(9.dp))
            if (error.isNotBlank()) Hint(error, true)
            if (networkError.isNotBlank()) Hint(networkError, messageIsError)
            MainButton(if (busy) "Conectando…" else "Ingresar", {
                if (id.length < 6 || password.isBlank()) error = "Completa ambos campos."
                else onLogin(id, password, remember)
            }, enabled = !busy)
            TextButton(onClick = onRecover, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("¿Olvidaste tu contraseña?", fontSize = 12.sp, color = OliveDeep)
            }
        }
        Spacer(Modifier.height(15.dp))
        OutlinedButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(51.dp),
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Olive)) {
            Text("Registrar funcionario", color = OliveDeep, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(17.dp))
        TextButton(onClick = {showServer = !showServer}, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Rounded.Settings, null, tint = Muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showServer) "Ocultar configuración" else "Configurar servidor API", color = Muted, fontSize = 12.sp)
        }
        if (showServer) {
            Spacer(Modifier.height(8.dp))
            Field("URL del servidor (IP de tu PC)", serverUrl, onServerUrl,
                "Ej. http://192.168.1.100:8080")
            Hint("En teléfono, escribe la IP real del PC conectado a la misma Wi-Fi. En el emulador de Android Studio, usa http://10.0.2.2:8080. El servidor CIVIS debe estar encendido. Ajusta el puerto si usa otro.")
            MainButton(if (busy) "Comprobando…" else "Comprobar conexión", onCheckServer, enabled = !busy)
        }
        Spacer(Modifier.height(8.dp))
        Hint("Conéctate al servidor de tu organización para iniciar sesión.")
    }
}

@Composable private fun Picker(label: String, options: List<String>, selected: String, hint: String = "Seleccionar", onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink)
    Spacer(Modifier.height(7.dp))
    Box {
        OutlinedButton(onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(53.dp),
            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Line)) {
            Text(selected.ifBlank { hint }, modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start, color = if (selected.isBlank()) Muted else Ink)
            Icon(Icons.Rounded.ExpandMore, null, tint = OliveDeep)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
    Spacer(Modifier.height(17.dp))
}

@Composable private fun RegisterScreen(
    onBack: () -> Unit, onRegister: (Officer) -> Unit,
    busy: Boolean, networkError: String
) {
    var id by remember { mutableStateOf("") }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var force by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var forceId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Screen("Registro de funcionario", "Validación de identidad institucional para habilitar el acceso.", onBack) {
        Hint("Solicita al administrador que autorice tu documento, fuerza, rango, ID institucional y correo. Al registrarte recibirás una contraseña temporal en ese correo para iniciar sesión.")
        if ((androidx.compose.ui.platform.LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Hint("Ejemplo autorizado en laboratorio\nDocumento 1111111111 · Nombres Juan · Apellidos Pérez\nFuerza Policía · Rango Patrullero\nID institucional POL-101\nCorreo funcionario@civis.test\nRequiere el servidor en laboratorio y el servicio de correo activo. La clave de prueba se consulta en Mailpit del PC (http://localhost:8025). Si ya registraste este documento, inicia sesión o recupera la contraseña.")
        }
        MinimalCard {
            Field("Identificación", id, { id = it.filter(Char::isDigit).take(12) },
                "Ej. 1111111111 (sin puntos)", keyboard = KeyboardType.Number)
            Field("Nombres", first, { first = it }, "Ej. Juan Carlos")
            Field("Apellidos", last, { last = it }, "Ej. Pérez Gómez")
            Picker("Fuerza pública", rankMap.keys.toList(), force, hint = "Ej. Policía") { force = it; rank = "" }
            Picker("Rango", rankMap[force].orEmpty(), rank, hint = if (force.isBlank()) "Selecciona primero la fuerza" else "Selecciona tu rango autorizado") { rank = it }
            Field("ID institucional de la fuerza", forceId, { forceId = it }, "Ej. POL-101")
            Field("Correo electrónico", email, { email = it.trim() },
                "Ej. juan.perez@institucion.gov.co", keyboard = KeyboardType.Email)
            if (error.isNotBlank()) Hint(error, true)
            if (networkError.isNotBlank()) Hint(networkError, true)
            MainButton(if (busy) "Verificando…" else "Solicitar registro", {
                error = if (id.length !in 6..12 || first.isBlank() || last.isBlank()
                    || force.isBlank() || rank.isBlank() || forceId.isBlank()
                    || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    "Verifica los campos obligatorios."
                } else ""
                if (error.isBlank()) onRegister(Officer(id,first.trim(),last.trim(),force,rank,forceId.trim(),email))
            }, enabled = !busy)
        }
        Spacer(Modifier.height(16.dp))
        Hint("El administrador debe autorizar previamente tu documento, fuerza, rango, ID institucional y correo.")
    }
}

@Composable private fun RecoverScreen(
    onBack: () -> Unit, onRecover: (String) -> Unit,
    busy: Boolean, message: String
) {
    var id by remember { mutableStateOf("") }
    Screen("Recuperar acceso", "Solicita una nueva clave temporal a tu correo registrado.", onBack) {
        MinimalCard {
            Field("Identificación", id, { id = it.filter(Char::isDigit).take(12) },
                "Número de documento", keyboard = KeyboardType.Number)
            MainButton(if (busy) "Procesando…" else "Solicitar recuperación", {
                onRecover(id)
            }, enabled = id.length >= 6 && !busy)
        }
        Spacer(Modifier.height(14.dp))
        if (message.isNotBlank()) Hint(message)
    }
}

@Composable private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, description: String, onClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(17.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(color = OlivePale, shape = RoundedCornerShape(11.dp)) {
                Icon(icon, null, tint = OliveDeep, modifier = Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable private fun HomeScreen(
    user: Officer, onScan: () -> Unit, onConsult: () -> Unit,
    onProfile: () -> Unit, scannerError: String,
    onManage: () -> Unit, onHistory: () -> Unit, onPassword: () -> Unit
) {
    Screen("Hola, ${user.first}", "Panel de operaciones · ${user.force}") {
        if(user.role == "ADMIN") {
            ActionRow(Icons.Rounded.AdminPanelSettings,"Administración","Ciudadanos, funcionarios, autorizaciones y reportes",onManage)
            Spacer(Modifier.height(12.dp))
        }
        ActionRow(Icons.Rounded.History,"Mis consultas","Historial de verificaciones realizadas",onHistory)
        Spacer(Modifier.height(12.dp))
        ActionRow(Icons.Rounded.Lock,"Cambiar contraseña","Actualiza tu clave de acceso",onPassword)
        Spacer(Modifier.height(12.dp))
        Surface(color = OliveDeep, shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.fillMaxWidth().padding(23.dp)) {
                Icon(Icons.Rounded.DocumentScanner, null, tint = Color(0xFFDBE7CE),
                    modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(23.dp))
                Text("Nueva verificación", color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 23.sp)
                Spacer(Modifier.height(7.dp))
                Text("Escanea una cédula o consulta el documento del ciudadano.",
                    color = Color(0xFFE5ECDE), fontSize = 13.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(23.dp))
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White,
                        contentColor = OliveDeep)) {
                    Icon(Icons.Rounded.QrCodeScanner, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Escanear documento", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (scannerError.isNotBlank()) { Spacer(Modifier.height(14.dp)); Hint(scannerError, true) }
        Spacer(Modifier.height(26.dp))
        Text("ACCESOS RÁPIDOS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = Muted, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(12.dp))
        ActionRow(Icons.Rounded.Search, "Consulta manual", "Buscar por número de identificación", onConsult)
        Spacer(Modifier.height(11.dp))
        ActionRow(Icons.Rounded.PersonOutline, "Mi perfil", "Información y cierre de sesión", onProfile)
        Spacer(Modifier.height(22.dp))
        Hint("Las consultas se registran en PostgreSQL. Los resultados son ficticios y no provienen de sistemas judiciales oficiales.")
    }
}

@Composable private fun ConsultScreen(scanned: String, onBack: () -> Unit, onSubmit: (String) -> Unit) {
    var id by remember(scanned) { mutableStateOf(
        if (scanned.matches(Regex("[0-9]{6,12}"))) scanned else ""
    ) }
    Screen("Consultar ciudadano", "Introduce y confirma la identificación antes de consultar.", onBack) {
        if (scanned.isNotBlank()) {
            Hint("Código leído: ${scanned.take(90)}${if (scanned.length > 90) "…" else ""}. Comprueba el número de cédula: PDF417 puede contener más datos.")
        }
        MinimalCard {
            Field("Número de identificación", id,
                {id = it.filter(Char::isDigit).take(12)},
                "Ej. 1000000011", keyboard = KeyboardType.Number)
            MainButton("Consultar estado", {onSubmit(id)}, enabled = id.length in 6..12)
        }
        Spacer(Modifier.height(18.dp))
        Hint("Registros ficticios disponibles: 1000000011 a 1000000020. La consulta requiere conexión con el servidor.")
    }
}

@Composable private fun ResultScreen(id: String, result: Lookup, onBack: () -> Unit, onNew: () -> Unit) {
    val citizen = result as? Lookup.Result
    val tone = if (citizen == null) Muted else if (citizen.required) Danger else Success
    val outcome = if (citizen == null) "Sin datos" else if (citizen.required) "Requerido" else "No requerido"
    Screen("Resultado", "Información devuelta por el servidor CIVIS.", onBack) {
        MinimalCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = OlivePale, shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Rounded.PersonOutline, null, tint = OliveDeep,
                        modifier = Modifier.padding(15.dp).size(33.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(citizen?.name?.ifBlank {"Identidad sin nombre"} ?: "Sin registro",
                        color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("CC $id", color = Muted, fontSize = 13.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 22.dp), color = Line)
            Text("ESTADO DE CONSULTA", fontSize = 10.sp,
                letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = Muted)
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(13.dp),
                color = tone.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, tone.copy(alpha = 0.25f))) {
                Row(Modifier.fillMaxWidth().padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (citizen == null) Icons.Rounded.HelpOutline
                        else if (citizen.required) Icons.Rounded.ErrorOutline
                        else Icons.Rounded.CheckCircle, null, tint = tone)
                    Spacer(Modifier.width(10.dp))
                    Text(outcome, fontSize = 16.sp, color = tone, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Resultado de prueba almacenado en PostgreSQL. No representa antecedentes judiciales reales.",
                color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.height(20.dp))
        MainButton("Nueva consulta", onNew)
        Spacer(Modifier.height(12.dp))
        Hint(if(LocalDemo.current) "Este resultado es únicamente académico y no debe usarse para actuaciones oficiales." else "Información del registro interno CIVIS. No acredita antecedentes de una fuente oficial.")
    }
}

@Composable private fun ProfileScreen(user: Officer, onBack: () -> Unit, onExit: () -> Unit) {
    Screen("Mi perfil", "Información del funcionario autenticado.", onBack) {
        MinimalCard {
            listOf(
                "Funcionario" to "${user.first} ${user.last}",
                "Identificación" to user.id,
                "Fuerza pública" to user.force,
                "Rango" to user.rank,
                "ID Fuerza" to user.forceId,
                "Correo electrónico" to user.email
            ).forEachIndexed { index, item ->
                Text(item.first, color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                Text(item.second, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (index < 5) HorizontalDivider(Modifier.padding(vertical = 15.dp), color = Line)
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(53.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Danger.copy(alpha = 0.35f))) {
            Icon(Icons.Rounded.Logout, null, tint = Danger)
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión", color = Danger, fontWeight = FontWeight.SemiBold)
        }
    }
}
