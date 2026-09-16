package com.example.quarkmdreader.data.model

data class QuarkFile(
    val fid: String,
    val fileName: String,
    val pdirFid: String,
    val isDir: Boolean,
    val size: Long = 0,
    val updatedAt: Long = 0,
    val formatType: String = ""
) {
    val isMarkdown: Boolean
        get() = !isDir && (fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true))

    val isImage: Boolean
        get() = !isDir && (
                fileName.endsWith(".png", ignoreCase = true) ||
                        fileName.endsWith(".jpg", ignoreCase = true) ||
                        fileName.endsWith(".jpeg", ignoreCase = true) ||
                        fileName.endsWith(".webp", ignoreCase = true) ||
                        fileName.endsWith(".gif", ignoreCase = true) ||
                        fileName.endsWith(".svg", ignoreCase = true)
                )
}
