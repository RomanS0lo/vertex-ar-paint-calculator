package com.vertex.ar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform