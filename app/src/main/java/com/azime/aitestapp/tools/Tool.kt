package com.azime.aitestapp.tools

/**
 * Base interface for agentic tools that the AI can invoke.
 *
 * ## Agentic Tool Framework
 *
 * This framework allows the on-device LLM to "call" device functions by:
 * 1. Detecting tool invocation patterns in the prompt (e.g., "check battery")
 * 2. Executing the appropriate tool
 * 3. Injecting tool results back into the conversation
 *
 * ## How Tools Work:
 *
 * ```
 * User: "What's my battery level?"
 *     ↓
 * ToolDetector: Matches "battery" → BatteryTool
 *     ↓
 * BatteryTool.execute() → "Battery: 85%, Charging"
 *     ↓
 * Response: "Your battery is at 85% and currently charging."
 * ```
 */
interface Tool {
    /** Unique identifier for this tool */
    val name: String
    
    /** Human-readable description of what this tool does */
    val description: String
    
    /** Keywords that trigger this tool */
    val triggerKeywords: List<String>
    
    /**
     * Execute the tool and return a result string.
     * This result will be provided to the LLM as context.
     */
    suspend fun execute(params: Map<String, String> = emptyMap()): ToolResult
}

/**
 * Result from a tool execution.
 */
sealed class ToolResult {
    /** Successful tool execution */
    data class Success(val data: String) : ToolResult()
    
    /** Tool execution failed */
    data class Error(val message: String) : ToolResult()
}
