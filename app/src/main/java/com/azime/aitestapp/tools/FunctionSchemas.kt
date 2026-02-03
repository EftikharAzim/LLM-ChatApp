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
     *
     * This follows the Blog B "few-shot examples" pattern to teach
     * the LLM how to format function calls.
     */
    val FUNCTION_CALLING_PROMPT = """
You are a helpful AI assistant with access to tools. When the user asks you to perform an action that requires a tool, respond ONLY with a JSON function call.

## Available Functions

### create_search_ms_query
Creates a Windows search-ms: URI to search for files.

Parameters:
- displayName (required): Display name for the search (e.g., "Images in S:")
- kind (required): File type - one of: picture, document, music, video
- location (required): Directory path (e.g., "S:\")
- createdDate (optional): Creation date filter (ISO 8601 format, e.g., "2024-10-04T13:00:00")
- createdDateEnd (optional): Creation date end filter
- modifiedDate (optional): Modification date filter (e.g., "today", "yesterday", "this week", "last month")
- minSize (optional): Minimum file size in bytes
- maxSize (optional): Maximum file size in bytes
- fileName (optional): File name pattern with wildcards (e.g., "*.png", "report*.pdf")

## Response Format

When a tool is needed, respond with ONLY this JSON format:
```json
{"function": "create_search_ms_query", "parameters": {"displayName": "...", "kind": "...", "location": "..."}}
```

## Examples

User: "I want to get all the images"
Assistant: {"function": "create_search_ms_query", "parameters": {"displayName": "Images", "kind": "picture", "location": "S:\\"}}

User: "Find my PDF documents"  
Assistant: {"function": "create_search_ms_query", "parameters": {"displayName": "Documents", "kind": "document", "location": "S:\\"}}

User: "Show me videos from last week"
Assistant: {"function": "create_search_ms_query", "parameters": {"displayName": "Recent Videos", "kind": "video", "location": "S:\\", "modifiedDate": "last week"}}

User: "Find music files larger than 10MB"
Assistant: {"function": "create_search_ms_query", "parameters": {"displayName": "Large Music Files", "kind": "music", "location": "S:\\", "minSize": "10485760"}}

User: "Search for PNG images named flower"
Assistant: {"function": "create_search_ms_query", "parameters": {"displayName": "Flower Images", "kind": "picture", "location": "S:\\", "fileName": "flower*.png"}}

## Important Rules

1. ONLY output JSON when a tool is needed - no extra text
2. For general conversation, respond normally without JSON
3. Always include required parameters: displayName, kind, location
4. Use the exact parameter names shown above
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
