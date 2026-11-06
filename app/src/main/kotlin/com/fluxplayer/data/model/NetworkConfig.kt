package com.fluxplayer.data.model

data class SmbConfig(
    val host: String = "",
    val share: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = ""
) {
    fun toSmbUrl(subPath: String = ""): String {
        val cleanHost = host.trim().trim('/')
        val cleanShare = share.trim().trim('/')
        val cleanSubPath = subPath.trim().trim('/')
        val path = if (cleanSubPath.isEmpty()) cleanShare else "$cleanShare/$cleanSubPath"
        return "smb://$cleanHost/$path"
    }
}

data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun toFullUrl(subPath: String = ""): String {
        val base = serverUrl.trim().trimEnd('/')
        val cleanSub = subPath.trim().trimStart('/')
        return if (cleanSub.isEmpty()) base else "$base/$cleanSub"
    }
}

