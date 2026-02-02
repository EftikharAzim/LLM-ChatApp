package com.azime.aitestapp.tools

import android.content.Context
import android.util.Log

/**
 * Registry that manages all available tools and handles tool detection/execution.
 *
 * ## Agentic Pattern:
 *
 * The ToolRegistry acts as the "tool dispatcher" in the agentic framework:
 *
 * 1. **Registration**: Tools register themselves with keywords
 * 2. **Detection**: User messages are scanned for tool triggers
 * 3. **Execution**: Matched tools are executed
 * 4. **Injection**: Tool results are injected into the prompt context
 *
 * ## Example Flow:
 *
 * ```kotlin
 * // User asks: "What's my battery?"
 * val tool = registry.detectTool("What's my battery?")
 * // Returns: BatteryTool
 *
 * val result = tool?.execute()
 * // Returns: "Battery: 85%, Charging, Good health"
 *
 * // Inject into prompt for LLM context
 * val enhancedPrompt = "[Tool Result: $result]\nUser: What's my battery?"
 * ```
 */
class ToolRegistry(context: Context) {

    companion object {
        private const val TAG = "ToolRegistry"
    }

    // All registered tools
    private val tools: MutableList<Tool> = mutableListOf()

    init {
        // Register all available tools
        registerTool(BatteryTool(context))
        
        Log.d(TAG, "Registered ${tools.size} tools: ${tools.map { it.name }}")
    }

    /**
     * Register a new tool.
     */
    fun registerTool(tool: Tool) {
        tools.add(tool)
        Log.d(TAG, "Registered tool: ${tool.name}")
    }

    /**
     * Get all registered tools.
     */
    fun getAllTools(): List<Tool> = tools.toList()

    /**
     * Detect if a user message should trigger any tool.
     * Returns the first matching tool, or null if no match.
     */
    fun detectTool(userMessage: String): Tool? {
        val messageLower = userMessage.lowercase()
        Log.i(TAG, "Scanning for tools: $userMessage")
        for (tool in tools) {
            for (keyword in tool.triggerKeywords) {
                if (messageLower.contains(keyword.lowercase())) {
                    Log.d(TAG, "Detected tool '${tool.name}' from keyword '$keyword'")
                    return tool
                }
            }
        }
        
        return null
    }

    /**
     * Execute a tool and return its result.
     */
    suspend fun executeTool(tool: Tool, params: Map<String, String> = emptyMap()): ToolResult {
        Log.d(TAG, "Executing tool: ${tool.name}")
        return tool.execute(params)
    }

    /**
     * Detect, execute, and return tool result if applicable.
     * Returns null if no tool was triggered.
     */
    suspend fun processMessage(userMessage: String): ToolResult? {
        val tool = detectTool(userMessage) ?: return null
        return executeTool(tool)
    }

    /**
     * Build a tool context string that can be injected into the LLM prompt.
     */
    fun buildToolContext(toolResult: ToolResult): String {
        return when (toolResult) {
            is ToolResult.Success -> """
                |[TOOL RESULT]
                |${toolResult.data}
                |[END TOOL RESULT]
            """.trimMargin()
            
            is ToolResult.Error -> """
                |[TOOL ERROR]
                |${toolResult.message}
                |[END TOOL ERROR]
            """.trimMargin()
        }
    }
}
