package com.example.network

import java.net.URI

/**
 * Robust IP and Endpoint Sanitizer and Validator for Local Network Trading Relays.
 *
 * Cleans user inputs (e.g. trimming whitespace, accidental quotes, trailing slashes,
 * missing http:// or ws:// protocols, and pasted JSON) for both WebSocket and HTTP Webhook endpoints.
 */
object NetworkUrlSanitizer {

    private val ipv4Pattern = Regex("""^(\d{1,3}\.){3}\d{1,3}(:\d+)?(/.*)?$""")
    private val jsonUrlExtractor = Regex("""(https?|wss?)://[^\s"',{}]+""")

    /**
     * Sanitizes and normalizes an HTTP Webhook Endpoint URL.
     * Defaults to port 5000 and /trade path when a bare IP or host is provided.
     */
    fun sanitizeHttpWebhookUrl(raw: String): String {
        var input = raw.trim()
        if (input.isBlank()) return ""

        // Strip surrounding quotes, backticks, brackets
        input = input.trim('"', '\'', '`', '{', '}', ' ', '\t', '\n', '\r')

        // If user accidentally pasted JSON or config block, extract first valid URL
        val match = jsonUrlExtractor.find(input)
        if (match != null) {
            input = match.value
        }

        // Remove trailing slashes
        while (input.endsWith("/")) {
            input = input.dropLast(1)
        }

        // Normalize protocol
        if (input.startsWith("ws://", ignoreCase = true)) {
            input = "http://" + input.substring(5)
        } else if (input.startsWith("wss://", ignoreCase = true)) {
            input = "https://" + input.substring(6)
        } else if (!input.startsWith("http://", ignoreCase = true) && !input.startsWith("https://", ignoreCase = true)) {
            input = "http://$input"
        }

        // Clean trailing slashes again after protocol fixing
        while (input.endsWith("/")) {
            input = input.dropLast(1)
        }

        return try {
            val uri = URI(input)
            val host = uri.host
            val port = uri.port
            val path = uri.path

            if (!host.isNullOrBlank()) {
                val effectivePort = if (port == -1) 5000 else port
                val effectivePath = if (path.isNullOrBlank() || path == "/") "/trade" else path
                val queryPart = if (uri.query.isNullOrBlank()) "" else "?${uri.query}"
                "${uri.scheme ?: "http"}://$host:$effectivePort$effectivePath$queryPart"
            } else {
                input
            }
        } catch (_: Exception) {
            input
        }
    }

    /**
     * Sanitizes and normalizes a WebSocket Server URL.
     * Defaults to port 8765 when a bare IP or host is provided.
     */
    fun sanitizeWebSocketUrl(raw: String): String {
        var input = raw.trim()
        if (input.isBlank()) return ""

        // Strip surrounding quotes, backticks, brackets
        input = input.trim('"', '\'', '`', '{', '}', ' ', '\t', '\n', '\r')

        val match = jsonUrlExtractor.find(input)
        if (match != null) {
            input = match.value
        }

        while (input.endsWith("/")) {
            input = input.dropLast(1)
        }

        if (input.startsWith("http://", ignoreCase = true)) {
            input = "ws://" + input.substring(7)
        } else if (input.startsWith("https://", ignoreCase = true)) {
            input = "wss://" + input.substring(8)
        } else if (!input.startsWith("ws://", ignoreCase = true) && !input.startsWith("wss://", ignoreCase = true)) {
            input = "ws://$input"
        }

        while (input.endsWith("/")) {
            input = input.dropLast(1)
        }

        return try {
            val uri = URI(input)
            val host = uri.host
            val port = uri.port
            val path = uri.path ?: ""

            if (!host.isNullOrBlank()) {
                val effectivePort = if (port == -1) 8765 else port
                val queryPart = if (uri.query.isNullOrBlank()) "" else "?${uri.query}"
                "${uri.scheme ?: "ws"}://$host:$effectivePort$path$queryPart"
            } else {
                input
            }
        } catch (_: Exception) {
            input
        }
    }
}
