package com.giathinh.uthconnect

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

private const val APP_PACKAGE_ID = "com.giathinh.uthconnect"
private const val PLAY_STORE_HOST = "play.google.com"
private const val PLAY_STORE_DETAILS_PATH = "/store/apps/details"
private const val PLAY_STORE_DETAILS_QUERY = "id=$APP_PACKAGE_ID"
private const val GITHUB_HOST = "github.com"
private const val GITHUB_RELEASES_PREFIX = "/ThinhNguyen2005/UTH-Connect/releases"
private const val ASCII_PRINTABLE_MIN = 0x21
private const val ASCII_PRINTABLE_MAX = 0x7E
private val UTH_HOSTS = setOf("ut.edu.vn", "uth.edu.vn")
private val FORBIDDEN_ENCODED_TOKENS = listOf("%2f", "%5c", "%2e", "%00")

internal object UpdateUrlValidator {
    fun isValid(url: String): Boolean {
        val uri = parseStrictUri(url)
        val hasValidStructure = uri != null && uri.isAbsolute && !uri.isOpaque && uri.rawFragment == null
        return if (hasValidStructure) {
            when (uri.scheme) {
                "market" -> isValidMarketUri(uri)
                "https" -> isValidHttpsUpdateUri(uri)
                else -> false
            }
        } else {
            false
        }
    }

    private fun parseStrictUri(url: String): URI? = if (url.isEmpty() || !isPrintableAsciiWithoutBackslash(url)) {
        null
    } else {
        try {
            URI(url)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun isPrintableAsciiWithoutBackslash(value: String): Boolean = value.all { ch ->
        ch.code in ASCII_PRINTABLE_MIN..ASCII_PRINTABLE_MAX && ch != '\\'
    }

    private fun extractStrictHost(uri: URI): String? {
        val host = uri.host
        val rawAuthority = uri.rawAuthority
        val hasNoUserInfoOrPort = uri.rawUserInfo == null && uri.userInfo == null && uri.port == -1
        val matchesAuthority = rawAuthority != null && rawAuthority.equals(host, ignoreCase = true)
        return if (host != null && hasNoUserInfoOrPort && matchesAuthority) {
            host.lowercase(Locale.ROOT)
        } else {
            null
        }
    }

    private fun isValidMarketUri(uri: URI): Boolean {
        val host = extractStrictHost(uri)
        return host == "details" &&
            uri.rawPath.isNullOrEmpty() &&
            uri.rawQuery == PLAY_STORE_DETAILS_QUERY
    }

    private fun isValidHttpsUpdateUri(uri: URI): Boolean {
        val host = extractStrictHost(uri)
        val rawPath = uri.rawPath
        return if (host == null || rawPath == null || !isSafeHierarchicalPath(rawPath)) {
            false
        } else {
            when (host) {
                PLAY_STORE_HOST -> rawPath == PLAY_STORE_DETAILS_PATH && uri.rawQuery == PLAY_STORE_DETAILS_QUERY
                GITHUB_HOST -> uri.rawQuery == null && isApprovedGithubReleasePath(rawPath)
                in UTH_HOSTS -> uri.rawQuery == null
                else -> false
            }
        }
    }

    private fun isSafeHierarchicalPath(rawPath: String): Boolean {
        val lower = rawPath.lowercase(Locale.ROOT)
        val hasValidPrefix = rawPath.startsWith("/") && rawPath.length > 1
        val hasNoTraversalTokens = "//" !in rawPath &&
            "://" !in rawPath &&
            FORBIDDEN_ENCODED_TOKENS.none { it in lower }
        val trimmedTrailingSlash = rawPath.trimEnd('/')
        val segments = if (trimmedTrailingSlash.isNotEmpty()) {
            trimmedTrailingSlash.substring(1).split('/')
        } else {
            emptyList()
        }
        val hasCleanSegments = segments.isNotEmpty() && segments.none { it == "." || it == ".." || it.isEmpty() }
        return hasValidPrefix && hasNoTraversalTokens && hasCleanSegments
    }

    private fun isApprovedGithubReleasePath(rawPath: String): Boolean {
        val normalized = rawPath.trimEnd('/')
        return normalized == GITHUB_RELEASES_PREFIX || normalized.startsWith("$GITHUB_RELEASES_PREFIX/")
    }
}
