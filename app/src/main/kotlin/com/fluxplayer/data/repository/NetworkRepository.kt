package com.fluxplayer.data.repository

import android.net.Uri
import com.fluxplayer.data.datasource.SmbDataSource
import com.fluxplayer.data.model.NetworkItem
import com.fluxplayer.data.model.SmbConfig
import com.fluxplayer.data.model.WebDavConfig
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor() {

    suspend fun testSmbConnection(config: SmbConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cifsContext = SmbDataSource.createCifsContext(config.username, config.password, config.domain)
            val rootUrl = config.toSmbUrl()
            val smbFile = SmbFile(if (rootUrl.endsWith('/')) rootUrl else "$rootUrl/", cifsContext)
            if (!smbFile.exists()) {
                return@withContext Result.failure(Exception("Share not accessible or does not exist"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSmbFiles(config: SmbConfig, subPath: String = ""): Result<List<NetworkItem>> = withContext(Dispatchers.IO) {
        try {
            val cifsContext = SmbDataSource.createCifsContext(config.username, config.password, config.domain)
            val folderUrl = config.toSmbUrl(subPath)
            val smbFolder = SmbFile(if (folderUrl.endsWith('/')) folderUrl else "$folderUrl/", cifsContext)

            val files = smbFolder.listFiles() ?: emptyArray()
            val items = files.map { file ->
                val isDir = file.isDirectory
                val name = file.name.trimEnd('/')
                val itemSubPath = if (subPath.isEmpty()) name else "$subPath/$name"
                NetworkItem(
                    name = name,
                    path = itemSubPath,
                    isDirectory = isDir,
                    sizeBytes = if (isDir) 0L else file.length(),
                    uri = Uri.parse(file.canonicalPath)
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testWebDavConnection(config: WebDavConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sardine: Sardine = OkHttpSardine()
            if (config.username.isNotEmpty()) {
                sardine.setCredentials(config.username, config.password)
            }
            val targetUrl = config.toFullUrl()
            val list = sardine.list(targetUrl)
            if (list.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("WebDAV endpoint empty or unreachable"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listWebDavFiles(config: WebDavConfig, subPath: String = ""): Result<List<NetworkItem>> = withContext(Dispatchers.IO) {
        try {
            val sardine: Sardine = OkHttpSardine()
            if (config.username.isNotEmpty()) {
                sardine.setCredentials(config.username, config.password)
            }
            val targetUrl = config.toFullUrl(subPath)
            val resources = sardine.list(targetUrl) ?: emptyList()

            // The first resource in WebDAV PROPFIND is usually the directory itself
            val items = resources.drop(1).map { res ->
                val name = res.name.trimEnd('/')
                val itemSubPath = if (subPath.isEmpty()) name else "$subPath/$name"
                val itemUrl = config.toFullUrl(itemSubPath)
                NetworkItem(
                    name = name,
                    path = itemSubPath,
                    isDirectory = res.isDirectory,
                    sizeBytes = if (res.isDirectory) 0L else res.contentLength,
                    uri = Uri.parse(itemUrl)
                )
            }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

