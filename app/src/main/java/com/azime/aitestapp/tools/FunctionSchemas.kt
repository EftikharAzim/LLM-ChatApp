package com.azime.aitestapp.tools

/**
 * Contains the function schema definitions for the LLM system prompt.
 *
 * These schemas tell the LLM what functions are available and how to call them.
 * Following the Blog B pattern: LLM generates JSON, system executes.
 */
object FunctionSchemas {

    /**
     * System prompt that defines available functions for the LLM.
     * Kept short for on-device Gemini Nano's limited context.
     */
    val FUNCTION_CALLING_PROMPT = """
You can search files by outputting JSON. Output ONLY the JSON, no other text.

Examples:
User: Find pictures
{"function":"create_search_ms_query","parameters":{"displayName":"Images","kind":"picture","location":"S:\\"}}

User: Get documents
{"function":"create_search_ms_query","parameters":{"displayName":"Documents","kind":"document","location":"S:\\"}}

User: Show videos
{"function":"create_search_ms_query","parameters":{"displayName":"Videos","kind":"video","location":"S:\\"}}

User: Find music
{"function":"create_search_ms_query","parameters":{"displayName":"Music","kind":"music","location":"S:\\"}}
""".trimIndent()

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
