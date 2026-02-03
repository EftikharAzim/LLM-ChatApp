package com.azime.aitestapp.tools

/**
 * Enum representing file categories for Windows search-ms URI generation.
 *
 * Each category maps to a display name and list of file extensions.
 * The extensions are used to build the `crumb` filter in the search-ms URI.
 *
 * ## Example:
 * ```
 * PICTURE.displayName → "Images"
 * PICTURE.extensions  → ["bmp", "gif", "heic", ...]
 * ```
 */
enum class FileCategory(
    val displayName: String,
    val extensions: List<String>
) {
    PICTURE(
        displayName = "Images",
        extensions = listOf(
            "bmp", "gif", "heic", "heif", "ico", "jpeg", "jpg",
            "png", "psd", "svg", "tif", "tiff", "webp"
        )
    ),
    
    MUSIC(
        displayName = "Music",
        extensions = listOf(
            "aac", "ac3", "aif", "aiff", "amr", "ape", "au",
            "flac", "m4a", "m4r", "mp3", "ogg", "wav", "wma"
        )
    ),
    
    MOVIE(
        displayName = "Videos",
        extensions = listOf(
            "3g2", "3gp", "asf", "avi", "divx", "flv", "m4v",
            "mkv", "mov", "mp4", "mpeg", "mpg", "ogv", "webm", "wmv"
        )
    ),
    
    DOCUMENT(
        displayName = "Documents",
        extensions = listOf(
            "7z", "csv", "doc", "docx", "epub", "gz", "htm", "html",
            "json", "log", "pdf", "ppt", "pptx", "rar", "rtf", "tar",
            "txt", "xls", "xlsx", "xml", "zip"
        )
    );

    companion object {
        /**
         * Parse a category string (case-insensitive) to FileCategory.
         * Supports natural language variations.
         *
         * @return FileCategory or null if not recognized
         */
        fun fromString(value: String): FileCategory? {
            val normalized = value.trim().lowercase()
            return when {
                // Picture variants
                normalized in listOf("picture", "pictures", "image", "images", "photo", "photos", "pic", "pics") -> PICTURE
                
                // Music variants
                normalized in listOf("music", "audio", "song", "songs", "sound", "sounds", "mp3", "mp3s") -> MUSIC
                
                // Movie variants
                normalized in listOf("movie", "movies", "video", "videos", "film", "films", "clip", "clips") -> MOVIE
                
                // Document variants
                normalized in listOf("document", "documents", "doc", "docs", "file", "files", "pdf", "pdfs", "text", "word") -> DOCUMENT
                
                else -> null
            }
        }
    }
}
