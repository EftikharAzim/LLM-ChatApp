package com.azime.aitestapp.tools

import android.util.Log
import java.net.URLEncoder

/**
 * Tool for generating Windows search-ms: URIs based on file categories.
 *
 * ## Purpose:
 * When connected to a remote Windows device, this tool generates search-ms: URIs
 * that can be used to search for specific file types (images, music, videos, documents).
 *
 * ## Example:
 * User: "I want to get all the images"
 * Output: search-ms:displayname=Images&crumb=*.bmp OR *.gif OR ...&crumb=location:S%3A%5C
 *
 * ## Architecture (Blog Principles):
 * - **Blog A (FSA)**: Output format is code-enforced, not AI-generated
 * - **Blog B (Separation)**: AI extracts category intent; this tool builds deterministic URI
 */
class SearchMsTool(
    private val deviceLocation: String = "S:\\"
) : Tool {

    companion object {
        private const val TAG = "SearchMsTool"
    }

    override val name: String = "search_files"

    override val description: String = "Generate Windows search URI to find files by category (images, music, videos, documents)"

    override val triggerKeywords: List<String> = listOf(
        // Picture keywords
        "picture", "pictures", "image", "images", "photo", "photos", "pic", "pics",
        // Music keywords
        "music", "audio", "song", "songs", "mp3", "sound",
        // Movie keywords
        "movie", "movies", "video", "videos", "film", "films",
        // Document keywords
        "document", "documents", "doc", "docs", "pdf", "pdfs", "file", "word",
        // General search keywords
        "find", "search", "get", "show", "all"
    )

    /**
     * Device location for search (pre-determined by connected device).
     */
    var location: String = deviceLocation
        private set

    /**
     * Update the device location (called when connecting to a new device).
     */
    fun updateLocation(newLocation: String) {
        location = newLocation
        Log.d(TAG, "Device location updated to: $newLocation")
    }

    override suspend fun execute(params: Map<String, String>): ToolResult {
        val categoryParam = params["category"]
        
        if (categoryParam.isNullOrBlank()) {
            Log.w(TAG, "No category parameter provided")
            return ToolResult.Error("Please specify what type of files you want to find (images, music, videos, or documents)")
        }

        val category = FileCategory.fromString(categoryParam)
        if (category == null) {
            Log.w(TAG, "Unrecognized category: $categoryParam")
            return ToolResult.Error("I don't recognize the file type '$categoryParam'. Try: images, music, videos, or documents")
        }

        return try {
            val searchUri = generate(category, location)
            Log.d(TAG, "Generated search-ms URI for ${category.displayName}: $searchUri")
            ToolResult.Success(searchUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate search URI", e)
            ToolResult.Error("Failed to generate search: ${e.message}")
        }
    }

    /**
     * Generate a Windows search-ms: URI for the given category and location.
     *
     * ## Format:
     * search-ms:displayname=<Category>&crumb=<extensions>&crumb=location:<encoded_path>
     *
     * @param category The file category to search for
     * @param searchLocation The location to search (e.g., "S:\")
     * @return A properly formatted search-ms: URI string
     */
    fun generate(category: FileCategory, searchLocation: String = location): String {
        // Build extension crumb: *.jpg OR *.png OR ...
        val extensionCrumb = category.extensions.joinToString(" OR ") { "*.$it" }
        
        // URL-encode the location (S:\ → S%3A%5C)
        val encodedLocation = URLEncoder.encode(searchLocation, "UTF-8")
        
        return "search-ms:displayname=${category.displayName}&crumb=$extensionCrumb&crumb=location:$encodedLocation"
    }

    /**
     * Detect which category is being requested from a user message.
     * This is a fallback when the AI doesn't extract the category properly.
     *
     * @param userMessage The user's natural language message
     * @return Detected FileCategory or null if no category detected
     */
    fun detectCategoryFromMessage(userMessage: String): FileCategory? {
        val messageLower = userMessage.lowercase()
        
        // Check for picture-related keywords
        if (messageLower.containsAny("picture", "pictures", "image", "images", "photo", "photos", "pic", "pics")) {
            return FileCategory.PICTURE
        }
        
        // Check for music-related keywords
        if (messageLower.containsAny("music", "audio", "song", "songs", "sound", "mp3")) {
            return FileCategory.MUSIC
        }
        
        // Check for video-related keywords
        if (messageLower.containsAny("movie", "movies", "video", "videos", "film", "films")) {
            return FileCategory.MOVIE
        }
        
        // Check for document-related keywords
        if (messageLower.containsAny("document", "documents", "doc", "docs", "pdf", "file", "word", "excel")) {
            return FileCategory.DOCUMENT
        }
        
        return null
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
