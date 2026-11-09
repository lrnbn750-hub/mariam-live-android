package com.mariam.live

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    // Temporary HTTP endpoint. Switch to HTTPS as soon as the API domain and TLS certificate are configured.
    private const val BASE_URL = "http://185.174.252.118:3000"

    data class Result(val status: Int, val body: JSONObject)

    fun post(path: String, payload: JSONObject): Result {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload.toString()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Result(status, if (text.isBlank()) JSONObject() else JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }
}
