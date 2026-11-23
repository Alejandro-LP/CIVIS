package com.civis.mobile

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

internal data class CitizenLookup(val status: String)

internal class CivisApi {
    var demoMode: Boolean = true
    var baseUrl: String = "http://10.0.2.2:8081"
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private fun request(path: String, method: String = "GET", data: JSONObject? = null, token: String? = null): JSONObject {
        val url = baseUrl.trim().trimEnd('/') + path
        if (!url.startsWith("http://") && !url.startsWith("https://")) throw IOException("La URL del servidor debe empezar por http:// o https://")
        val builder = try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            throw IOException("La dirección del servidor no es válida. Ejemplo de formato: http://10.0.2.2:8081", e)
        }
        if(token != null) builder.header("Authorization","Bearer $token")
        if(method != "GET") builder.method(method, (data ?: JSONObject()).toString().toRequestBody(jsonType))
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: SocketTimeoutException) {
            throw IOException("El servidor $baseUrl no respondió a tiempo. Comprueba la IP, el puerto y la conexión con el PC en Configurar servidor API.", e)
        } catch (e: ConnectException) {
            throw IOException("No se pudo conectar con $baseUrl. Inicia el servidor CIVIS y comprueba su dirección en Configurar servidor API.", e)
        } catch (e: UnknownHostException) {
            throw IOException("No se encontró el servidor $baseUrl. Revisa la dirección y tu conexión de red.", e)
        }
        return response.use { r ->
            val body = r.body?.string().orEmpty()
            if(!r.isSuccessful) {
                val message = when(r.code){401->"Identificación o contraseña incorrectas. Usa la contraseña recibida por correo. Demo123 solo funciona con la cuenta de laboratorio.";403->"El administrador debe autorizar tu documento, fuerza, rango, ID institucional y correo antes del registro.";404->"No se encontró la ruta de CIVIS. Comprueba que la URL y el puerto correspondan al servidor CIVIS.";409->"El usuario ya existe. Inicia sesión o recupera tu contraseña.";503->"El servidor de correo no está disponible. El administrador debe configurarlo para completar el registro.";else->"Error HTTP ${r.code}"}
                throw IOException(runCatching { JSONObject(body).optString("message",message) }.getOrDefault(message))
            }
            if(body.isBlank()) JSONObject() else JSONObject(body)
        }
    }
    fun checkConnection(): Boolean {
        val result = request("/api/health")
        if (result.optString("status") != "UP" || !result.has("demo")) {
            throw IOException("Esta dirección no devuelve el estado de CIVIS. Comprueba la URL y el puerto del servidor.")
        }
        return result.getBoolean("demo")
    }
    fun login(id:String, pass:String): Pair<String, Officer> {
        val r=request("/api/auth/login","POST",JSONObject().put("identification",id).put("password",pass))
        val o=r.getJSONObject("officer")
        demoMode=r.optBoolean("demo",true)
        return r.getString("token") to Officer(o.getString("identification"),o.getString("firstName"),o.getString("lastName"),o.getString("force"),o.getString("rank"),o.getString("forceId"),o.getString("email"),o.optString("role","OFFICER"))
    }
    fun register(o:Officer) {
        request("/api/auth/register","POST",JSONObject().put("identification",o.id).put("firstName",o.first).put("lastName",o.last).put("force",o.force).put("rank",o.rank).put("forceId",o.forceId).put("email",o.email))
    }
    fun recover(id:String) { request("/api/auth/recover","POST",JSONObject().put("identification",id)) }
    fun lookup(token:String,id:String):CitizenLookup {
        val r = request("/api/citizens/$id/status",token=token)
        return CitizenLookup(r.getString("status"))
    }
    fun logout(token:String) { request("/api/auth/logout","POST",JSONObject(),token) }
    fun changePassword(token:String,old:String,new:String) {
        request("/api/auth/change-password","POST",JSONObject().put("oldPassword",old).put("newPassword",new),token)
    }
    fun rows(token:String,path:String):List<JSONObject> {
        val r = bytes(token,path).toString(Charsets.UTF_8)
        val array = if(r.trimStart().startsWith("[")) JSONArray(r) else JSONObject(r).getJSONArray("content")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
    fun summary(token:String):JSONObject = request("/api/admin/summary",token=token)
    fun save(token:String,path:String,data:JSONObject,method:String="POST") { request(path,method,data,token) }
    fun pdf(token:String,required:Boolean?):ByteArray = bytes(token,"/api/admin/report.pdf"+(required?.let{"?required=$it"}?:""))
    private fun bytes(token:String,path:String):ByteArray {
        val request=Request.Builder().url(baseUrl.trim().trimEnd('/')+path).header("Authorization","Bearer $token").build()
        return client.newCall(request).execute().use { response ->
            if(!response.isSuccessful) throw IOException(if(response.code==401) "La sesión ha expirado. Vuelve a iniciar sesión." else "No se pudo obtener la información (${response.code})")
            response.body?.bytes() ?: throw IOException("Respuesta vacía del servidor")
        }
    }
}
