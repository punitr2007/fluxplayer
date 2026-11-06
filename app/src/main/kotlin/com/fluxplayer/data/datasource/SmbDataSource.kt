package com.fluxplayer.data.datasource

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import java.io.EOFException
import java.io.IOException
import java.util.Properties

@OptIn(UnstableApi::class)
class SmbDataSource(
    private val cifsContext: CIFSContext
) : BaseDataSource(/* isNetwork = */ true) {

    private var dataSpec: DataSpec? = null
    private var smbFile: SmbFile? = null
    private var smbRaf: SmbRandomAccessFile? = null
    private var bytesRemaining: Long = 0
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        transferInitializing(dataSpec)

        try {
            val smbUriString = dataSpec.uri.toString()
            val file = SmbFile(smbUriString, cifsContext)
            smbFile = file

            if (!file.exists()) {
                throw IOException("SMB file does not exist: $smbUriString")
            }

            val fileLength = file.length()
            val raf = SmbRandomAccessFile(file, "r")
            smbRaf = raf

            if (dataSpec.position > fileLength) {
                throw EOFException("Position ${dataSpec.position} exceeds file length $fileLength")
            }

            raf.seek(dataSpec.position)

            bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                dataSpec.length
            } else {
                fileLength - dataSpec.position
            }
        } catch (e: Exception) {
            throw IOException("Failed to open SMB stream: ${e.message}", e)
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val raf = smbRaf ?: throw IOException("SmbRandomAccessFile not open")
        val bytesToRead = if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            minOf(length.toLong(), bytesRemaining).toInt()
        } else {
            length
        }

        val bytesRead = raf.read(buffer, offset, bytesToRead)
        if (bytesRead == -1) {
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                throw EOFException("End of input reached with $bytesRemaining bytes remaining")
            }
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }

        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        dataSpec = null
        try {
            smbRaf?.close()
        } finally {
            smbRaf = null
            smbFile = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    companion object {
        fun createCifsContext(username: String = "", password: String = "", domain: String = ""): CIFSContext {
            val props = Properties().apply {
                setProperty("jcifs.smb.client.minVersion", "SMB202")
                setProperty("jcifs.smb.client.maxVersion", "SMB311")
                setProperty("jcifs.smb.client.dfs.disabled", "true")
                setProperty("jcifs.smb.client.responseTimeout", "15000")
            }
            val baseContext = BaseContext(PropertyConfiguration(props))
            return if (username.isNotEmpty()) {
                val auth = NtlmPasswordAuthenticator(domain, username, password)
                baseContext.withCredentials(auth)
            } else {
                baseContext.withGuestCrendentials()
            }
        }
    }
}

