package com.example.seitasv2

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Ajusta si cambias puerto/host del backend

const val BASE_URL = "http://192.168.0.17:5000/api"


/* -------------------------------------------------------
 *  Helpers HTTP básicos
 * ------------------------------------------------------ */

private fun addAuthIfAny(conn: HttpURLConnection, context: Context) {
    val token = SessionManager(context).getToken()
    if (!token.isNullOrBlank()) {
        conn.setRequestProperty("Authorization", "Bearer $token")
    }
}

private fun readBody(conn: HttpURLConnection): String {
    val stream = try {
        if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
    } catch (_: Exception) {
        conn.errorStream ?: conn.inputStream
    }
    return stream?.use { BufferedReader(InputStreamReader(it)).readText() }.orEmpty()
}

/** Lee body y lanza Exception si el código no es 2xx.
 *  Si el body es JSON con {error:"..."}, lanza ese mensaje. */
private fun readBodyOrThrow(conn: HttpURLConnection, allowEmpty: Boolean = false): String {
    val code = conn.responseCode
    val body = readBody(conn)

    if (code !in 200..299) {
        val msg = try {
            val obj = JSONObject(body)
            obj.optString("error", body.ifBlank { "HTTP $code" })
        } catch (_: Exception) {
            body.ifBlank { "HTTP $code" }
        }
        throw Exception(msg)
    }
    if (!allowEmpty && body.isBlank()) {
        Log.w("API", "Respuesta vacía (HTTP $code)")
    }
    return body
}

/* -------------------------------------------------------
 *  Auth: Login / Registro
 * ------------------------------------------------------ */

suspend fun loginRequest(context: Context, email: String, password: String): Pair<String, JSONObject> =
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/login")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        try {
            conn.outputStream.use { it.writer().use { w -> w.write(payload) } }
            val res = JSONObject(readBodyOrThrow(conn))
            val token = res.getString("token")
            val user = res.getJSONObject("user")
            token to user
        } finally {
            conn.disconnect()
        }
    }

suspend fun registerRequest(context: Context, email: String, password: String): Pair<String, JSONObject> =
    withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL/register")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        val payload = JSONObject().apply {
            put("email", email)
            put("password", password)
        }.toString()
        try {
            conn.outputStream.use { it.writer().use { w -> w.write(payload) } }
            val res = JSONObject(readBodyOrThrow(conn))
            val token = res.getString("token")
            val user = res.getJSONObject("user")
            token to user
        } finally {
            conn.disconnect()
        }
    }

/* -------------------------------------------------------
 *  Reusables GET/POST/PUT/DELETE con token
 * ------------------------------------------------------ */

suspend fun httpGet(context: Context, urlStr: String): String = withContext(Dispatchers.IO) {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
        addAuthIfAny(this, context)
    }
    try { readBodyOrThrow(conn) } finally { conn.disconnect() }
}

suspend fun httpDelete(context: Context, urlStr: String): String = withContext(Dispatchers.IO) {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = "DELETE"
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Accept", "application/json")
        addAuthIfAny(this, context)
    }
    try { readBodyOrThrow(conn, allowEmpty = true) } finally { conn.disconnect() }
}

suspend fun httpPost(context: Context, urlStr: String, jsonBody: String): String =
    httpWriteWithBody(context, "POST", urlStr, jsonBody)

suspend fun httpPut(context: Context, urlStr: String, jsonBody: String): String =
    httpWriteWithBody(context, "PUT", urlStr, jsonBody)

private suspend fun httpWriteWithBody(
    context: Context,
    method: String,
    urlStr: String,
    jsonBody: String
): String = withContext(Dispatchers.IO) {
    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        doOutput = true
        connectTimeout = 10000
        readTimeout = 10000
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        addAuthIfAny(this, context)
    }
    try {
        conn.outputStream.use { it.writer().use { w -> w.write(jsonBody) } }
        readBodyOrThrow(conn)
    } finally {
        conn.disconnect()
    }
}

/* -------------------------------------------------------
 *  Lecciones (CRUD)
 * ------------------------------------------------------ */

/* -------------------------------------------------------
 *  Gestos (GET todos)
 * ------------------------------------------------------ */

suspend fun getGestos(context: Context): List<GestoDB> {
    val body = httpGet(context, "$BASE_URL/gestos")
    val arr = JSONArray(body)
    val list = mutableListOf<GestoDB>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val id = obj.getInt("id")
        val nombre = obj.getString("nombre")
        val datosArr = obj.getJSONArray("datos")
        val datos = (0 until datosArr.length()).map { datosArr.getDouble(it).toFloat() }
        list.add(GestoDB(id, nombre, datos))
    }
    return list
}
