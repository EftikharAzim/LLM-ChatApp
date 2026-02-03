package com.azime.aitestapp.tools

import android.util.Log
import org.json.JSONObject

/**
 * Parser for detecting and extracting function calls from LLM output.
 *
 * ## Blog B Pattern Implementation:
 *
 * The LLM generates structured JSON describing which function to call:
 * ```json
 * {"function": "create_search_ms_query", "parameters": {"kind": "picture", "location": "S:\\"}}
 * ```
 *
 * This parser:
 * 1. Detects if the response contains a function call JSON
 * 2. Extracts the function name and parameters
 * 3. Returns a structured FunctionCall object
 */
object FunctionCallParser {

    private const val TAG = "FunctionCallParser"

    /**
     * Represents a parsed function call from LLM output.
     */
    data class FunctionCall(
        val functionName: String,
        val parameters: Map<String, String?>
    )

    /**
     * Try to parse a function call from the LLM response.
     *
     * Handles multiple formats:
     * - Direct JSON: {"function": "...", "parameters": {...}}
     * - JSON in code block: ```json {...} ```
     * - JSON with extra text before/after
     *
     * @return FunctionCall if detected, null otherwise
     */
    fun parse(response: String): FunctionCall? {
        val trimmed = response.trim()
        
        // Try to extract JSON from code block first
        val jsonString = extractJsonFromCodeBlock(trimmed) 
            ?: extractJsonFromResponse(trimmed)
            ?: return null

        return try {
            parseJsonToFunctionCall(jsonString)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse function call JSON: ${e.message}")
            null
        }
    }

    /**
     * Extract JSON from markdown code block.
     * Handles: ```json {...} ``` or ``` {...} ```
     */
    private fun extractJsonFromCodeBlock(response: String): String? {
        // Pattern for ```json ... ``` or ``` ... ```
        val codeBlockRegex = Regex("""```(?:json)?\s*(\{.*?\})\s*```""", RegexOption.DOT_MATCHES_ALL)
        return codeBlockRegex.find(response)?.groupValues?.get(1)?.trim()
    }

    /**
     * Extract JSON object from response that might have text around it.
     */
    private fun extractJsonFromResponse(response: String): String? {
        // Find the first { and last matching }
        val start = response.indexOf('{')
        if (start == -1) return null

        var depth = 0
        for (i in start until response.length) {
            when (response[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return response.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    /**
     * Parse JSON string into FunctionCall object.
     */
    private fun parseJsonToFunctionCall(jsonString: String): FunctionCall? {
        val json = JSONObject(jsonString)
        
        // Get function name (support both "function" and "name" keys)
        val functionName = json.optString("function")
            .ifEmpty { json.optString("name") }
            .ifEmpty { return null }

        // Get parameters
        val paramsObj = json.optJSONObject("parameters") ?: JSONObject()
        val parameters = mutableMapOf<String, String?>()
        
        paramsObj.keys().forEach { key ->
            val value = paramsObj.opt(key)
            parameters[key] = when {
                value == null || value == JSONObject.NULL -> null
                else -> value.toString()
            }
        }

        Log.d(TAG, "Parsed function call: $functionName with ${parameters.size} params")
        return FunctionCall(functionName, parameters)
    }

    /**
     * Check if a response looks like it might contain a function call.
     * Quick check before attempting full parse.
     */
    fun containsFunctionCall(response: String): Boolean {
        val lower = response.lowercase()
        return response.contains("{") && 
               (lower.contains("\"function\"") || lower.contains("\"name\"")) &&
               lower.contains("\"parameters\"")
    }
}
