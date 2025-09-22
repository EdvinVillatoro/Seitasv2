package com.example.seitasv2

data class Leccion(
    val id: Int,
    val descripcion: String,
    val videoUrl: String,
    val tips: String,
    val idCategoria: Int?,
    val estado: String,
    val fechaGeneracion: String,
    var completada: Boolean
)
