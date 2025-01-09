package com.example.tcc2.models

data class Server(
    val url: String,
    val lat: Double,
    val lon: Double,
    val name: String,
    val sponsor: String,
    var distance: Double = Double.MAX_VALUE,
    var latency: Long = Long.MAX_VALUE

)
