package com.azime.aitestapp.tools

/**
 * Contains the function schema definitions for the LLM system prompt.
 *
 * These schemas tell the LLM what functions are available and how to call them.
 * Following the Blog B pattern: LLM generates JSON, system executes.
 */
object FunctionSchemas {

    /**
     * System prompt for general conversation.
     * Gemini Nano can't handle JSON function calling prompts, so we use keyword detection instead.
     */
    val FUNCTION_CALLING_PROMPT = """
You are a helpful assistant. When the user asks to find files, respond with a helpful message.
""".trimIndent()

    /**
     * Detect if user message is asking for a file search and extract the file type.
     * Returns the file category if detected, null otherwise.
     */
    fun detectSearchIntent(message: String): FileCategory? {
        val lower = message.lowercase()
        
        return when {
            lower.contains("picture") || lower.contains("image") || lower.contains("photo") -> FileCategory.PICTURE
            lower.contains("document") || lower.contains("pdf") || lower.contains("doc") -> FileCategory.DOCUMENT
            lower.contains("video") || lower.contains("movie") -> FileCategory.MOVIE
            lower.contains("music") || lower.contains("audio") || lower.contains("song") -> FileCategory.MUSIC
            else -> null
        }
    }

    /**
     * Mapping of kind values to FileCategory enum.
     */
    val KIND_TO_CATEGORY = mapOf(
        "picture" to FileCategory.PICTURE,
        "image" to FileCategory.PICTURE,
        "images" to FileCategory.PICTURE,
        "photo" to FileCategory.PICTURE,
        "document" to FileCategory.DOCUMENT,
        "documents" to FileCategory.DOCUMENT,
        "doc" to FileCategory.DOCUMENT,
        "music" to FileCategory.MUSIC,
        "audio" to FileCategory.MUSIC,
        "video" to FileCategory.MOVIE,
        "videos" to FileCategory.MOVIE,
        "movie" to FileCategory.MOVIE
    )
}
