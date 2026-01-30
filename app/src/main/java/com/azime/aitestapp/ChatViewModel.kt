package com.azime.aitestapp

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ViewModel for the Chat screen, managing conversation state and AI interactions.
 *
 * ## Architecture Overview:
 *
 * This ViewModel follows **unidirectional data flow (UDF)**:
 *
 * ```
 * User Action → ViewModel → PromptService → ML Kit → ViewModel → UI State → Compose
 *                  ↑                                                    │
 *                  └────────────────────────────────────────────────────┘
 * ```
 *
 * ## Key Design Decisions:
 *
 * 1. **Sealed Class States**: We use sealed classes for UI state to ensure exhaustive
 *    handling in Compose. This prevents bugs where a state goes unhandled.
 *
 * 2. **Conversation Memory**: We maintain the last [MAX_CONTEXT_MESSAGES] messages
 *    for context. This balances memory usage with conversation coherence.
 *    - Too few messages = AI loses context
 *    - Too many messages = Slow inference + potential context overflow
 *
 * 3. **Token Debouncing**: During streaming, we update UI every [TOKEN_DEBOUNCE_COUNT]
 *    tokens to reduce recomposition overhead. This is critical for smooth scrolling.
 *
 * 4. **Job Cancellation**: We track the current generation job so users can potentially
 *    cancel long-running inferences (future feature).
 *
 * ## Memory Considerations:
 *
 * - Chat history is kept in memory (simple for this implementation)
 * - For production, you'd want Room database persistence
 * - We clear streaming state on completion to avoid memory leaks
 */
class ChatViewModel(
    application: Application, 
    private val promptService: PromptService
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        
        /**
         * Maximum number of messages to include in context.
         * 6 messages = 3 exchanges (user + assistant pairs)
         * This keeps context focused and inference fast.
         */
        private const val MAX_CONTEXT_MESSAGES = 6
        
        /**
         * Update UI every N tokens during streaming.
         * Lower = smoother updates but more recomposition overhead.
         * Higher = choppier updates but better performance.
         */
        private const val TOKEN_DEBOUNCE_COUNT = 2
        
        /**
         * Maximum time to wait for inference before timing out.
         */
        private const val INFERENCE_TIMEOUT_MS = 30_000L
    }

    // ========== State Flows ==========

    /** All messages in the conversation */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** Current UI state (idle, generating, error) */
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Model status (checking, downloading, ready, error) */
    val modelStatus: StateFlow<ModelStatus> = promptService.modelStatus

    /** Current user input text (two-way binding with UI) */
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // ========== Internal State ==========

    /** Current generation job for cancellation support */
    private var currentGenerationJob: Job? = null

    /** Token accumulator for debouncing */
    private var tokenBuffer = StringBuilder()
    private var tokenCount = 0

    // ========== Initialization ==========

    init {
        Log.d(TAG, "ChatViewModel initialized, starting PromptService...")
        initializePromptService()
    }

    /**
     * Initialize the ML Kit prompt service asynchronously.
     *
     * This downloads the model if needed and prepares for inference.
     * UI observes [modelStatus] to show loading/download progress.
     */
    private fun initializePromptService() {
        viewModelScope.launch {
            promptService.initialize()
        }
    }

    // ========== User Actions ==========

    /**
     * Update the input text field.
     * Called by Compose TextField's onValueChange.
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /**
     * Send a message and generate AI response.
     *
     * This is the main entry point when user taps "Send".
     *
     * ## Flow:
     * 1. Add user message to history
     * 2. Clear input field
     * 3. Start AI generation with streaming
     * 4. Accumulate tokens and update UI
     * 5. Add final AI message to history
     *
     * ## Error Handling:
     * - If model not ready, show error state
     * - If generation fails, show fallback response
     * - If timeout occurs, show partial response + error
     */
    fun sendMessage() {
        val message = _inputText.value.trim()
        if (message.isEmpty()) return

        // Don't allow sending while generating
        if (_uiState.value is ChatUiState.Generating) {
            Log.w(TAG, "Already generating, ignoring send")
            return
        }

        // Check if model is ready
        if (modelStatus.value !is ModelStatus.Ready) {
            Log.w(TAG, "Model not ready: ${modelStatus.value}")
            _uiState.value = ChatUiState.Error("Model not ready. Please wait for download to complete.")
            return
        }

        Log.d(TAG, "Sending message: ${message.take(50)}...")

        // Add user message to history
        val userMessage = ChatMessage(
            content = message,
            role = ChatRole.USER
        )
        _messages.value = _messages.value + userMessage

        // Clear input
        _inputText.value = ""

        // Start generation
        generateAIResponse(message)
    }

    /**
     * Generate AI response with streaming tokens.
     *
     * Uses coroutine Flow to collect streaming tokens and update UI progressively.
     */
    private fun generateAIResponse(userMessage: String) {
        // Cancel any existing generation
        currentGenerationJob?.cancel()

        // Reset token buffer
        tokenBuffer = StringBuilder()
        tokenCount = 0

        currentGenerationJob = viewModelScope.launch {
            try {
                // Get conversation context (last N messages)
                val context = _messages.value
                    .filter { it.role != ChatRole.SYSTEM }
                    .takeLast(MAX_CONTEXT_MESSAGES)
                    .dropLast(1) // Exclude the message we just added (it's in userMessage)

                Log.d(TAG, "Starting generation with ${context.size} context messages")

                // Add placeholder streaming message
                val streamingMessage = ChatMessage(
                    content = "",
                    role = ChatRole.ASSISTANT,
                    isStreaming = true
                )
                _messages.value = _messages.value + streamingMessage

                // Collect streaming response with timeout
                val result = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
                    promptService.generateResponse(userMessage, context)
                        .onStart {
                            Log.d(TAG, "Stream started")
                            _uiState.value = ChatUiState.Generating("")
                        }
                        .onCompletion { cause ->
                            Log.d(TAG, "Stream completed. Cause: $cause")
                        }
                        .catch { e ->
                            Log.e(TAG, "Stream error", e)
                            handleStreamError(e)
                        }
                        .collect { token ->
                            handleToken(token)
                        }
                }

                // Handle timeout
                if (result == null) {
                    Log.w(TAG, "Generation timed out after ${INFERENCE_TIMEOUT_MS}ms")
                    handleTimeout()
                } else {
                    // Finalize the message
                    finalizeResponse()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during generation", e)
                handleStreamError(e)
            }
        }
    }

    /**
     * Handle a single streaming token.
     *
     * We accumulate tokens and update UI periodically for performance.
     */
    private fun handleToken(token: String) {
        tokenBuffer.append(token)
        tokenCount++

        // Update UI every N tokens for performance
        if (tokenCount % TOKEN_DEBOUNCE_COUNT == 0) {
            updateStreamingMessage(tokenBuffer.toString())
        }
    }

    /**
     * Update the streaming message in the UI.
     */
    private fun updateStreamingMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.isNotEmpty()) {
            val lastIndex = currentMessages.lastIndex
            val lastMessage = currentMessages[lastIndex]
            if (lastMessage.role == ChatRole.ASSISTANT && lastMessage.isStreaming) {
                currentMessages[lastIndex] = lastMessage.copy(content = content)
                _messages.value = currentMessages
                _uiState.value = ChatUiState.Generating(content)
            }
        }
    }

    /**
     * Finalize the AI response after streaming completes.
     */
    private fun finalizeResponse() {
        val finalContent = tokenBuffer.toString().ifEmpty { 
            PromptService.FALLBACK_RESPONSE 
        }

        Log.d(TAG, "Finalizing response: ${finalContent.take(50)}... (${tokenCount} tokens)")

        val currentMessages = _messages.value.toMutableList()
        if (currentMessages.isNotEmpty()) {
            val lastIndex = currentMessages.lastIndex
            val lastMessage = currentMessages[lastIndex]
            if (lastMessage.role == ChatRole.ASSISTANT) {
                currentMessages[lastIndex] = lastMessage.copy(
                    content = finalContent,
                    isStreaming = false
                )
                _messages.value = currentMessages
            }
        }

        _uiState.value = ChatUiState.Idle
        tokenBuffer = StringBuilder()
        tokenCount = 0
    }

    /**
     * Handle streaming errors gracefully.
     */
    private fun handleStreamError(error: Throwable) {
        val partialContent = tokenBuffer.toString()
        
        if (partialContent.isNotEmpty()) {
            // Keep partial response with error indicator
            val errorContent = "$partialContent\n\n[Response interrupted: ${error.message}]"
            updateStreamingMessage(errorContent)
        } else {
            // No content yet, show fallback
            updateStreamingMessage(PromptService.FALLBACK_RESPONSE)
        }

        finalizeResponse()
        _uiState.value = ChatUiState.Error(error.message ?: "Generation failed")
        
        // Auto-clear error after delay
        viewModelScope.launch {
            delay(3000)
            if (_uiState.value is ChatUiState.Error) {
                _uiState.value = ChatUiState.Idle
            }
        }
    }

    /**
     * Handle generation timeout.
     */
    private fun handleTimeout() {
        val partialContent = tokenBuffer.toString()
        
        if (partialContent.isNotEmpty()) {
            // Keep partial response with timeout indicator
            val timeoutContent = "$partialContent\n\n[Response timed out]"
            updateStreamingMessage(timeoutContent)
        } else {
            updateStreamingMessage(PromptService.FALLBACK_RESPONSE)
        }

        finalizeResponse()
    }

    /**
     * Clear the chat history.
     * Could be exposed to UI via a "Clear Chat" button.
     */
    fun clearChat() {
        Log.d(TAG, "Clearing chat history")
        _messages.value = emptyList()
        _uiState.value = ChatUiState.Idle
    }

    /**
     * Dismiss any error state.
     */
    fun dismissError() {
        if (_uiState.value is ChatUiState.Error) {
            _uiState.value = ChatUiState.Idle
        }
    }

    // ========== Lifecycle ==========

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel cleared, cleaning up...")
        currentGenerationJob?.cancel()
        promptService.cleanup()
    }
}