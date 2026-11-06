package com.fluxplayer.data.datasource

import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import java.nio.charset.StandardCharsets

@OptIn(UnstableApi::class)
object WebDavDataSourceFactory {

    fun create(
        username: String = "",
        password: String = "",
        userAgent: String = "FluxPlayer/1.0"
    ): DataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)

        if (username.isNotEmpty()) {
            val credentials = "$username:$password"
            val authHeader = "Basic " + Base64.encodeToString(
                credentials.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
            httpFactory.setDefaultRequestProperties(mapOf("Authorization" to authHeader))
        }

        return httpFactory
    }
}

