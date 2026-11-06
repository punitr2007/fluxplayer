package com.fluxplayer.data.model

import android.net.Uri

data class NetworkItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val uri: Uri
) {
    val isVideo: Boolean
        get() {
            if (isDirectory) return false
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in VIDEO_EXTENSIONS
        }

    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    companion object {
        val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "webm", "avi", "mov", "wmv", "flv", "m4v", "ts", "m2ts", "3gp", "vob"
        )
    }
}

