package com.example.seitasv2.models

data class Palabra(
    val id: Int,
    val palabra: String,
    val pista: String = "",
    val minijuego: String = "ahorcado"
)

