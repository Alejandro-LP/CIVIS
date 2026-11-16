package com.civis.mobile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
private fun ManagementPage(title:String,onBack:()->Unit,content:@Composable ColumnScope.()->Unit) {
    Surface(color=MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            TextButton(onClick=onBack) { Text("← Volver") }
            Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            content()
        }
    }
}

@Composable
internal fun PasswordScreen(api:CivisApi,token:String,onBack:()->Unit,onChanged:()->Unit) {
    var old by remember { mutableStateOf("") };var new by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") };var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") };val scope=rememberCoroutineScope()
    ManagementPage("Cambiar contraseña",onBack) {
        Text("Usa entre 10 y 72 caracteres, con mayúscula, minúscula y número. Al guardar, se cerrarán tus sesiones.")
        listOf("Contraseña actual" to old,"Nueva contraseña" to new,"Confirmar contraseña" to confirmation).forEachIndexed { i,pair ->
            OutlinedTextField(pair.second,{ when(i){0->old=it;1->new=it;else->confirmation=it} },label={Text(pair.first)},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        }
        if(message.isNotBlank()) Text(message,color=MaterialTheme.colorScheme.error)
        Button(enabled=!busy && old.isNotBlank() && new.isNotBlank(),onClick={
            if(new!=confirmation) message="Las contraseñas no coinciden" else {
                busy=true;scope.launch {
                    try { withContext(Dispatchers.IO){api.changePassword(token,old,new)};onChanged() }
                    catch(e:Exception){message=e.message?:"No se pudo guardar"} finally {busy=false}
                }
            }
        }) {Text(if(busy) "Guardando…" else "Actualizar contraseña")}
    }
}

@Composable
internal fun HistoryScreen(api:CivisApi,token:String,onBack:()->Unit,admin:Boolean=false) {
    var rows by remember { mutableStateOf<List<JSONObject>>(emptyList()) };var page by remember {mutableStateOf(0)}
    var refresh by remember {mutableStateOf(0)};var busy by remember {mutableStateOf(true)};var error by remember {mutableStateOf("")}
    LaunchedEffect(page,refresh) {
        busy=true;error=""
        try {rows=withContext(Dispatchers.IO){api.rows(token,(if(admin) "/api/admin/audit" else "/api/history")+"?page=$page&size=30")}}
        catch(e:Exception){error=e.message?:"No se pudo cargar";rows=emptyList()} finally {busy=false}
    }
    ManagementPage(if(admin) "Auditoría de consultas" else "Mis consultas",onBack) {
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(error.isNotBlank()) {Text(error,color=MaterialTheme.colorScheme.error);TextButton(onClick={refresh++}){Text("Reintentar")}}
        if(!busy && error.isBlank() && rows.isEmpty()) Text("No hay consultas en esta página.")
        rows.forEach {r->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp)) {
                Text(r.optString("citizenId"),fontWeight=FontWeight.Bold)
                Text(r.optString("outcome").replace('_',' '))
                Text(r.optString("occurredAt"));if(admin) Text("Funcionario: "+r.optString("officerId"))
            }}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            TextButton(enabled=page>0 && !busy,onClick={page--}){Text("Anterior")}
            Text("Página ${page+1}",Modifier.padding(top=12.dp))
            TextButton(enabled=rows.size==30 && !busy,onClick={page++}){Text("Siguiente")}
        }
    }
}

@Composable
internal fun ManagementScreen(api:CivisApi,token:String,actorId:String,onBack:()->Unit) {
    var tab by remember {mutableStateOf("Ciudadanos")};var rows by remember {mutableStateOf<List<JSONObject>>(emptyList())}
    var summary by remember {mutableStateOf(JSONObject())};var refresh by remember {mutableStateOf(0)}
    var busy by remember {mutableStateOf(false)};var message by remember {mutableStateOf("")}
    var query by remember {mutableStateOf("")};var editor by remember {mutableStateOf<JSONObject?>(null)}
    var deleting by remember {mutableStateOf<JSONObject?>(null)};var filter by remember {mutableStateOf<Boolean?>(null)}
    val scope=rememberCoroutineScope();val context=LocalContext.current
    val download=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) {uri ->
        if(uri!=null) {busy=true;scope.launch {
            try {withContext(Dispatchers.IO){
                val bytes=api.pdf(token,filter)
                val output=context.contentResolver.openOutputStream(uri)?:error("No se pudo abrir el archivo")
                output.use{it.write(bytes)}
            };message="Reporte PDF guardado"} catch(e:Exception){message=e.message?:"No se pudo descargar"} finally {busy=false}
        }}
    }
    fun update(path:String,data:JSONObject,method:String="POST") {
        busy=true;message="";scope.launch {
            try {withContext(Dispatchers.IO){api.save(token,path,data,method)};editor=null;deleting=null;refresh++;message="Cambios guardados"}
            catch(e:Exception){message=e.message?:"No se pudo guardar"} finally {busy=false}
        }
    }
    LaunchedEffect(tab,refresh) {
        if(tab=="Auditoría") return@LaunchedEffect
        busy=true;message=""
        try {
            val data=withContext(Dispatchers.IO){
                api.summary(token) to api.rows(token,when(tab){"Funcionarios"->"/api/admin/officers";"Autorizaciones"->"/api/admin/registry";else->"/api/admin/citizens"})
            };summary=data.first;rows=data.second
        } catch(e:Exception){message=e.message?:"No se pudo cargar";rows=emptyList()} finally {busy=false}
    }
    if(tab=="Auditoría") {HistoryScreen(api,token,{tab="Ciudadanos"},true);return}
    ManagementPage("Administración",onBack) {
        Text("${summary.optLong("citizens")} ciudadanos · ${summary.optLong("required")} requeridos\n${summary.optLong("officers")} funcionarios · ${summary.optLong("lookups")} consultas")
        listOf(listOf("Ciudadanos","Funcionarios"),listOf("Autorizaciones","Auditoría")).forEach {group ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {group.forEach {label ->
                FilterChip(selected=tab==label,onClick={tab=label;editor=null;query=""},label={Text(label)},enabled=!busy)
            }}
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank()) Text(message)
        TextButton(enabled=!busy,onClick={refresh++}){Text("Actualizar datos")}
        if(editor!=null) {
            RecordEditor(tab,editor!!,busy,onCancel={editor=null},onSave={data->update(if(tab=="Ciudadanos") "/api/admin/citizens" else "/api/admin/registry",data)})
        } else {
            OutlinedTextField(query,{query=it},label={Text("Buscar por documento o nombre")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            if(tab!="Funcionarios") Button(enabled=!busy,onClick={editor=JSONObject()}) {Text(if(tab=="Ciudadanos") "Añadir ciudadano" else "Autorizar funcionario")}
            if(tab=="Ciudadanos") {
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Todos" to null,"Requeridos" to true,"Otros" to false).forEach {(label,value)->FilterChip(filter==value,{filter=value},label={Text(label)})}
                }
                OutlinedButton(enabled=!busy,onClick={download.launch("civis-reporte.pdf")}){Text("Guardar reporte PDF del filtro")}
            }
            val visible=rows.filter {r-> (tab!="Ciudadanos" || filter==null || r.optBoolean("required")==filter) &&
                listOf("identification","firstName","lastName","forceId","email").any{r.optString(it).contains(query,true)} }
            if(!busy && visible.isEmpty()) Text("No hay registros para mostrar.")
            visible.forEach {r->
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Text(r.optString("identification"),fontWeight=FontWeight.Bold)
                    if(tab!="Autorizaciones") Text(r.optString("firstName")+" "+r.optString("lastName"))
                    if(tab=="Ciudadanos") {
                        Text(if(r.optBoolean("required")) "REQUERIDO" else "NO REQUERIDO")
                        Row {
                            TextButton(enabled=!busy,onClick={editor=JSONObject(r.toString())}){Text("Editar")}
                            TextButton(enabled=!busy,onClick={deleting=r}){Text("Eliminar")}
                        }
                    } else {
                        Text(r.optString("force")+" · "+r.optString("rank"));Text(r.optString("email"))
                        Text(if(r.optBoolean("active")) "Activo" else "Inactivo")
                        if(tab=="Autorizaciones") TextButton(enabled=!busy,onClick={editor=JSONObject(r.toString())}){Text("Editar autorización")}
                        else {
                            Text("Rol: "+r.optString("role"))
                            if(r.optString("identification")!=actorId) Row {
                                TextButton(enabled=!busy,onClick={update("/api/admin/officers/"+r.optString("identification"),JSONObject().put("role",r.optString("role")).put("active",!r.optBoolean("active")),"PATCH")}){Text(if(r.optBoolean("active")) "Desactivar" else "Activar")}
                                TextButton(enabled=!busy,onClick={update("/api/admin/officers/"+r.optString("identification"),JSONObject().put("role",if(r.optString("role")=="ADMIN") "OFFICER" else "ADMIN").put("active",r.optBoolean("active")),"PATCH")}){Text("Cambiar rol")}
                            }
                        }
                    }
                }}
            }
        }
    }
    deleting?.let {record -> AlertDialog(onDismissRequest={if(!busy) deleting=null},title={Text("Eliminar ciudadano")},text={Text("¿Eliminar el registro ${record.optString("identification")}? El historial de consultas se conservará.")},
        confirmButton={TextButton(enabled=!busy,onClick={update("/api/admin/citizens/"+record.optString("identification"),JSONObject(),"DELETE")}){Text("Eliminar")}},
        dismissButton={TextButton(enabled=!busy,onClick={deleting=null}){Text("Cancelar")}}) }
}

@Composable
private fun RecordEditor(tab:String,initial:JSONObject,busy:Boolean,onCancel:()->Unit,onSave:(JSONObject)->Unit) {
    val citizen=tab=="Ciudadanos"
    val fields=if(citizen) listOf("identification" to "Identificación *","firstName" to "Nombres *","lastName" to "Apellidos *",
        "birthDate" to "Nacimiento (AAAA-MM-DD)","birthPlace" to "Lugar de nacimiento","issueDate" to "Expedición (AAAA-MM-DD)",
        "issuePlace" to "Lugar de expedición","rh" to "RH (+ o -)","bloodGroup" to "Grupo sanguíneo (O, A, B, AB)","heightCm" to "Estatura (cm)")
        else listOf("identification" to "Identificación *","force" to "Fuerza *","rank" to "Rango *","forceId" to "ID institucional *","email" to "Correo *")
    val values=remember(initial){mutableStateMapOf<String,String>().apply{fields.forEach{(key,_)->put(key,if(initial.isNull(key)) "" else initial.optString(key))}}}
    var flag by remember(initial){mutableStateOf(initial.optBoolean(if(citizen) "required" else "active",!citizen))}
    var error by remember(initial){mutableStateOf("")}
    fields.forEach {(key,label)->OutlinedTextField(values[key].orEmpty(),{values[key]=it},label={Text(label)},singleLine=true,
        enabled=!busy && (key!="identification" || !initial.has("identification")),modifier=Modifier.fillMaxWidth())}
    Row {Checkbox(flag,{flag=it},enabled=!busy);Text(if(citizen) "Ciudadano requerido" else "Autorización activa",Modifier.padding(top=12.dp))}
    if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
    Row {
        Button(enabled=!busy,onClick={
            val required=if(citizen) listOf("identification","firstName","lastName") else fields.map{it.first}
            error=when {
                required.any{values[it].isNullOrBlank()} -> "Completa los campos obligatorios"
                !values["identification"].orEmpty().matches(Regex("\\d{6,12}")) -> "La identificación debe tener entre 6 y 12 dígitos"
                citizen && !values["heightCm"].isNullOrBlank() && values["heightCm"]?.toIntOrNull()==null -> "La estatura debe ser un número"
                else -> ""
            }
            if(error.isBlank()) {
                val data=JSONObject();fields.forEach{(key,_)->
                    val value=values[key].orEmpty().trim()
                    data.put(key,if(value.isBlank()) JSONObject.NULL else if(key=="heightCm") value.toInt() else value)
                };data.put(if(citizen) "required" else "active",flag);onSave(data)
            }
        }){Text("Guardar")}
        TextButton(enabled=!busy,onClick=onCancel){Text("Cancelar")}
    }
}
