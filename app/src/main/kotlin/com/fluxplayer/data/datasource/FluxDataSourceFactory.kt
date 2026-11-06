package com.fluxplayer.data.datasource

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import jcifs.CIFSContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class FluxDataSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context
) : DataSource.Factory {

    @Volatile
    private var currentCifsContext: CIFSContext? = null
    @Volatile
    private var webDavUsername: String = ""
    @Volatile
    private var webDavPassword: String = ""

    suspend fun updateSmbCredentials(username: String, password: String, domain: String = "") = withContext(Dispatchers.IO) {
        currentCifsContext = SmbDataSource.createCifsContext(username, password, domain)
    }

    fun setCifsContext(cifsContext: CIFSContext) {
        currentCifsContext = cifsContext
    }

    fun updateWebDavCredentials(username: String, password: String) {
        webDavUsername = username
        webDavPassword = password
    }

    private fun getOrCreateCifsContext(): CIFSContext {
        return currentCifsContext ?: synchronized(this) {
            currentCifsContext ?: SmbDataSource.createCifsContext().also { currentCifsContext = it }
        }
    }

    override fun createDataSource(): DataSource {
        val defaultFactory = DefaultDataSource.Factory(context)

        return object : DataSource {
            private var activeSource: DataSource? = null

            override fun open(dataSpec: DataSpec): Long {
                val scheme = dataSpec.uri.scheme?.lowercase() ?: ""

                val delegate = when {
                    scheme == "smb" -> {
                        SmbDataSource(getOrCreateCifsContext())
                    }
                    scheme == "webdav" || scheme == "webdavs" -> {
                        val httpScheme = if (scheme == "webdavs") "https" else "http"
                        val httpUri = dataSpec.uri.buildUpon().scheme(httpScheme).build()
                        val factory = WebDavDataSourceFactory.create(webDavUsername, webDavPassword)
                        return factory.createDataSource().also { activeSource = it }
                            .open(dataSpec.buildUpon().setUri(httpUri).build())
                    }
                    (scheme == "http" || scheme == "https") && webDavUsername.isNotEmpty() -> {
                        WebDavDataSourceFactory.create(webDavUsername, webDavPassword).createDataSource()
                    }
                    else -> {
                        defaultFactory.createDataSource()
                    }
                }
                activeSource = delegate
                return delegate.open(dataSpec)
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                return activeSource?.read(buffer, offset, length) ?: -1
            }

            override fun getUri(): Uri? {
                return activeSource?.uri
            }

            override fun close() {
                try {
                    activeSource?.close()
                } finally {
                    activeSource = null
                }
            }

            override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
                // Handled in delegate
            }
        }
    }
}
