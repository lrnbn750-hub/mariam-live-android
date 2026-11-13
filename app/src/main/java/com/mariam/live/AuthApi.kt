package com.mariam.live

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    // HTTPS API endpoint protected by Caddy TLS.
    private const val BASE_URL = "https://nokiraq.duckdns.org"

    fun absoluteUrl(path: String): String =
        if (path.startsWith("https://") || path.startsWith("http://")) path
        else BASE_URL + (if (path.startsWith("/")) path else "/$path")

    data class Result(val status: Int, val body: JSONObject)

    private fun parseBody(text: String): JSONObject {
        if (text.isBlank()) return JSONObject()
        return try {
            JSONObject(text)
        } catch (_: Exception) {
            val preview = text.trim().take(180).replace(Regex("\\s+"), " ")
            JSONObject().put(
                "error",
                if (preview.contains("<!DOCTYPE", true) || preview.contains("<html", true))
                    "الخادم أعاد صفحة HTML بدل JSON. تحقق من نشر مسار الملف الشخصي على الخادم."
                else "استجابة غير صالحة من الخادم: $preview"
            )
        }
    }

    fun get(path: String, token: String): Result {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Result(status, parseBody(text))
        } finally {
            connection.disconnect()
        }
    }

    fun post(path: String, payload: JSONObject, token: String): Result {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
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


    fun put(path: String, payload: JSONObject, token: String): Result {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "PUT"
            connectTimeout = 20000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
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

    fun delete(path: String, token: String): Result {
        val connection = (URL("$BASE_URL$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $token")
        }
        return try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            Result(status, if (text.isBlank()) JSONObject() else JSONObject(text))
        } finally {
            connection.disconnect()
        }
    }

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
