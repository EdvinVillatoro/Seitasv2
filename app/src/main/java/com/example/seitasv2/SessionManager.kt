package com.example.seitasv2

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun saveSession(token: String, id: Int, email: String, tipo: String) {
        prefs.edit()
            .putString("token", token)
            .putInt("user_id", id)
            .putString("email", email)
            .putString("tipo", tipo)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()
    fun clear() { prefs.edit().clear().apply() }
}
