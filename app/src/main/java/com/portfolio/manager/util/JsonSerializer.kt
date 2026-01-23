package com.portfolio.manager.util

import kotlinx.serialization.json.Json

object JsonSerializer {
    val instance: Json = Json { ignoreUnknownKeys = true }
}
