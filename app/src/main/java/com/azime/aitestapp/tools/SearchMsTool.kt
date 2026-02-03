package com.azime.aitestapp.tools

import android.util.Log
import java.net.URLEncoder
import java.nio.file.Paths

/**
 * Tool for generating Windows search-ms: URIs with full parameter support.
 *
 * ## MCP-Style Function Definition:
 *
 * Creates a Windows search-ms: URI to search for files. Supports:
 * - File type filtering (picture, document, music, video)
 * - Date filtering (creation and modification dates)
 * - Size filtering (min/max bytes)
 * - Filename pattern matching with wildcards
 *
 * ## Blog B Pattern:
 * - LLM generates JSON with function name + parameters
 * - This tool executes deterministically with those parameters
 * - Returns the exact search-ms: URI string
 */
class SearchMsTool(
    private val defaultLocation: String = "S:\\"
) : Tool {

    companion object {
        private const val TAG = "SearchMsTool"
    }

    override val name: String = "create_search_ms_query"

    override val description: String = """
        Creates a Windows search-ms: URI to search for files.
        Parameters: displayName, kind (picture/document/music/video), location,
        createdDate, createdDateEnd, modifiedDate, minSize, maxSize, fileName
    """.trimIndent()

    override val triggerKeywords: List<String> = listOf(
        "picture", "pictures", "image", "images", "photo", "photos",
        "music", "audio", "song", "songs", "mp3",
        "movie", "movies", "video", "videos", "film",
        "document", "documents", "doc", "docs", "pdf", "file", "files",
        "find", "search", "get", "show", "all"
    )

    /**
     * Current device location (pre-determined by connected device).
     */
    var location: String = defaultLocation
        private set

    /**
     * Update the device location (called when connecting to a new device).
     */
    fun updateLocation(newLocation: String) {
        location = newLocation
        Log.d(TAG, "Device location updated to: $newLocation")
    }

    /**
     * Execute the tool with parameters from parsed JSON function call.
     *
     * Expected parameters:
     * - displayName: Display name for the search
     * - kind: File type (picture, document, music, video)
     * - location: Search directory path
     * - createdDate: Creation date filter (ISO 8601)
     * - createdDateEnd: Creation date end filter
     * - modifiedDate: Modification date filter
     * - minSize: Minimum file size in bytes
     * - maxSize: Maximum file size in bytes
     * - fileName: File name pattern with wildcards
     */
    override suspend fun execute(params: Map<String, String>): ToolResult {
        return try {
            val query = createSearchMsQuery(params)
            Log.d(TAG, "Generated search-ms query: $query")
            ToolResult.Success(query)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create search-ms query", e)
            ToolResult.Error("Error creating search-ms query: ${e.message}")
        }
    }

    /**
     * Create the search-ms: query string from parameters.
     * Mirrors the C# MCP SearchMSTool implementation.
     */
    fun createSearchMsQuery(params: Map<String, String>): String {
        val queryParts = mutableListOf<String>()

        // Display name
        val displayName = params["displayName"] ?: "Search Results"
        queryParts.add("displayname=${URLEncoder.encode(displayName, "UTF-8")}")

        // Kind filter (file type)
        val kind = params["kind"]
        if (!kind.isNullOrBlank()) {
            queryParts.add("&crumb=kind:=$kind")
        }

        // Date created filter
        val createdDate = params["createdDate"]
        if (!createdDate.isNullOrBlank()) {
            // Check if it's a valid date format or relative date
            if (isIsoDate(createdDate)) {
                queryParts.add("datecreated:>$createdDate")
            }
        }

        // Date created end filter
        val createdDateEnd = params["createdDateEnd"]
        if (!createdDateEnd.isNullOrBlank()) {
            queryParts.add("datecreated:<$createdDateEnd")
        }

        // Date modified filter
        val modifiedDate = params["modifiedDate"]
        if (!modifiedDate.isNullOrBlank()) {
            queryParts.add("datemodified:$modifiedDate")
        }

        // Size filters
        val minSize = params["minSize"]
        if (!minSize.isNullOrBlank()) {
            queryParts.add("size:>$minSize")
        }

        val maxSize = params["maxSize"]
        if (!maxSize.isNullOrBlank()) {
            queryParts.add("size:<$maxSize")
        }

        // File name pattern filter
        val fileName = params["fileName"]
        if (!fileName.isNullOrBlank()) {
            val queryPrefix = URLEncoder.encode("filename:~", "UTF-8")
            val querySuffix = URLEncoder.encode(" OR System.Generic.String:", "UTF-8")
            queryParts.add("&crumb=$queryPrefix$fileName$querySuffix$fileName")
        }

        // Location (use provided or default)
        val searchLocation = params["location"]?.replace('/', '\\') ?: location
        queryParts.add("&crumb=location:${URLEncoder.encode(searchLocation, "UTF-8")}")

        // Combine all parts
        return "search-ms:" + queryParts.joinToString(" ")
    }

    /**
     * Check if a string looks like an ISO 8601 date.
     */
    private fun isIsoDate(date: String): Boolean {
        return date.matches(Regex("""\d{4}-\d{2}-\d{2}.*"""))
    }

    /**
     * Detect file category from user message (fallback for keyword matching).
     */
    fun detectCategoryFromMessage(userMessage: String): FileCategory? {
        val messageLower = userMessage.lowercase()
        
        return when {
            messageLower.containsAny("picture", "pictures", "image", "images", "photo", "photos", "pic", "pics") -> FileCategory.PICTURE
            messageLower.containsAny("music", "audio", "song", "songs", "sound", "mp3") -> FileCategory.MUSIC
            messageLower.containsAny("movie", "movies", "video", "videos", "film", "films") -> FileCategory.MOVIE
            messageLower.containsAny("document", "documents", "doc", "docs", "pdf", "file", "word", "excel") -> FileCategory.DOCUMENT
            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
