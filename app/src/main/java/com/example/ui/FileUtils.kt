package com.example.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object FileUtils {
    /**
     * Copies a Uri's content to the app's internal files directory and returns the absolute path/uri of the copied file.
     * Use a prefix or folder to categorize if needed (e.g., "avatars", "videos", etc.)
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, folderName: String): String? {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = when {
                mimeType?.contains("image/png") == true -> "png"
                mimeType?.contains("image/jpeg") == true -> "jpg"
                mimeType?.contains("image/webp") == true -> "webp"
                mimeType?.contains("video/mp4") == true -> "mp4"
                mimeType?.contains("video/3gpp") == true -> "3gp"
                mimeType?.contains("audio/mpeg") == true -> "mp3"
                mimeType?.contains("audio/mp4") == true -> "m4a"
                else -> {
                    // Try to extract from Uri path
                    val lastSegment = uri.lastPathSegment ?: ""
                    if (lastSegment.contains(".")) {
                        lastSegment.substringAfterLast(".")
                    } else {
                        // fallback based on mime
                        if (mimeType?.startsWith("image/") == true) "jpg"
                        else if (mimeType?.startsWith("video/") == true) "mp4"
                        else if (mimeType?.startsWith("audio/") == true) "mp3"
                        else "bin"
                    }
                }
            }

            val dir = File(context.filesDir, folderName)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val uniqueFileName = "${UUID.randomUUID()}.$extension"
            val destFile = File(dir, uniqueFileName)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                return Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Gets the file name without extension from a given Uri.
     */
    fun getFileNameWithoutExtension(context: Context, uri: Uri): String {
        var result: String? = null
        val uriString = uri.toString()

        // Strategy 1: If the URI contains a file name with an extension inside its decoded string
        val fileFromUri = extractFromUriString(uriString)
        if (fileFromUri != null) {
            result = fileFromUri
        }

        // Strategy 2: If the authority is com.android.providers.media.documents
        if (result == null && uriString.contains("/document/")) {
            try {
                val documentId = Uri.decode(uriString.substringAfter("/document/"))
                if (documentId.contains(":")) {
                    val parts = documentId.split(":")
                    if (parts.size >= 2) {
                        val type = parts[0]
                        val idStr = parts[1]
                        val id = idStr.toLongOrNull()
                        if (id != null) {
                            val contentUri = when (type) {
                                "video" -> android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                "audio" -> android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                "image" -> android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                else -> null
                            }
                            if (contentUri != null) {
                                val mediaUri = android.content.ContentUris.withAppendedId(contentUri, id)
                                val proj = arrayOf(android.provider.MediaStore.MediaColumns.TITLE, android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                                context.contentResolver.query(mediaUri, proj, null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val titleIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.TITLE)
                                        val nameIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                                        var foundName: String? = null
                                        if (titleIdx != -1) foundName = cursor.getString(titleIdx)
                                        if ((foundName == null || foundName.all { it.isDigit() }) && nameIdx != -1) {
                                            foundName = cursor.getString(nameIdx)
                                        }
                                        if (foundName != null && foundName.isNotEmpty() && !foundName.all { it.isDigit() }) {
                                            result = foundName
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (documentId.startsWith("raw:") || documentId.startsWith("/")) {
                    val path = documentId.substringAfter("raw:")
                    val lastSlash = path.lastIndexOf('/')
                    if (lastSlash != -1) {
                        result = path.substring(lastSlash + 1)
                    } else {
                        result = path
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Strategy 3: Try standard content query for TITLE / DISPLAY_NAME
        if (result == null && uri.scheme == "content") {
            try {
                val proj = arrayOf(android.provider.MediaStore.MediaColumns.TITLE, android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val titleIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.TITLE)
                        val nameIdx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                        var foundName: String? = null
                        if (titleIdx != -1) foundName = cursor.getString(titleIdx)
                        if ((foundName == null || foundName.all { it.isDigit() }) && nameIdx != -1) {
                            foundName = cursor.getString(nameIdx)
                        }
                        if (foundName != null && foundName.isNotEmpty() && !foundName.all { it.isDigit() }) {
                            result = foundName
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Strategy 4: Try standard OpenableColumns query
        if (result == null && uri.scheme == "content") {
            try {
                val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            val foundName = cursor.getString(nameIdx)
                            if (foundName != null && foundName.isNotEmpty() && !foundName.all { it.isDigit() }) {
                                result = foundName
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Strategy 5: Fallback to the decoded last path segment
        if (result == null) {
            val decodedPath = uri.path?.let { Uri.decode(it) }
            if (decodedPath != null) {
                val lastSlash = decodedPath.lastIndexOf('/')
                if (lastSlash != -1) {
                    result = decodedPath.substring(lastSlash + 1)
                } else {
                    result = decodedPath
                }
            }
        }

        // Final formatting
        if (result == null) return ""
        
        // Remove file extension if present
        val nameWithoutExt = if (result!!.contains(".")) {
            result!!.substringBeforeLast(".")
        } else {
            result!!
        }

        // If the resulting name is just numbers, but we have some other info (like last segment of uri path), try that
        if (nameWithoutExt.all { it.isDigit() } || nameWithoutExt.isEmpty()) {
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null && lastSegment.isNotEmpty() && !lastSegment.all { it.isDigit() }) {
                return if (lastSegment.contains(".")) lastSegment.substringBeforeLast(".") else lastSegment
            }
        }

        return nameWithoutExt
    }

    private fun extractFromUriString(uriString: String): String? {
        val decoded = Uri.decode(uriString)
        val extensions = listOf(".mp4", ".mkv", ".avi", ".3gp", ".webm", ".mov", ".mp3", ".m4a", ".wav", ".ogg", ".png", ".jpg", ".jpeg", ".webp")
        for (ext in extensions) {
            if (decoded.contains(ext, ignoreCase = true)) {
                val index = decoded.indexOf(ext, ignoreCase = true)
                var start = index
                while (start > 0) {
                    val char = decoded[start - 1]
                    if (char == '/' || char == ':' || char == '\\' || char == '?' || char == '&' || char == '=') {
                        break
                    }
                    start--
                }
                val fileNameWithExt = decoded.substring(start, index + ext.length)
                if (fileNameWithExt.isNotEmpty()) {
                    return fileNameWithExt
                }
            }
        }
        return null
    }
}
