package com.azime.aitestapp

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Service layer for handling on-device LLM inference using ML Kit GenAI Prompt API.
 *
 * ## Architecture Decisions:
 *
 * 1. **Single GenerativeModel**: We maintain a single `GenerativeModel` instance for the 
 *    lifetime of the service. Creating models is expensive and keeps the model loaded.
 *
 * 2. **Kotlin Flow-based API**: ML Kit's Prompt API uses Kotlin Flow for streaming,
 *    which we directly expose to the ViewModel.
 *
 * 3. **Context Management**: Multi-turn conversations are handled by building the full
 *    conversation history into each prompt string.
 *
 * ## Performance Considerations:
 *
 * - Model stays in memory after first inference (~500MB-1GB RAM)
 * - First inference has "cold start" latency (~2-4 seconds)
 * - Subsequent inferences are faster (~200-500ms to first token)
 * - Token streaming rate: ~20-40 tokens/second on flagship chips
 */
class PromptService {

    companion object {
        private const val TAG = "PromptService"
        
        /** Fallback response when inference fails */
        const val FALLBACK_RESPONSE = "I'm having trouble responding right now. Please try again."
        
        /** System prompt that defines the AI's behavior and function calling format */
        private const val SYSTEM_PROMPT = """You are WeatherBot, an AI assistant.

IMPORTANT RULES:
1. ONLY output JSON for weather-related questions
2. The ONLY function you can use is "get_weather" - do NOT invent new functions
3. For ALL non-weather questions, respond with plain text ONLY (no JSON)

JSON FORMAT (only for weather questions):
{"function": "get_weather", "parameters": {"city": "<city_name>"}}

CORRECT EXAMPLES:
User: What is the weather like in Berlin today?
Assistant: {"function": "get_weather", "parameters": {"city": "Berlin"}}

User: How's the weather in Tokyo?
Assistant: {"function": "get_weather", "parameters": {"city": "Tokyo"}}

User: Who is Donald Trump?
Assistant: Donald Trump is a businessman and politician who served as the 45th President of the United States.

User: Tell me a joke
Assistant: Why don't scientists trust atoms? Because they make up everything!

WRONG (never do this):
User: Who is Elon Musk?
Assistant: {"function": "get_info", "parameters": {"person": "Elon Musk"}}
^ This is WRONG because get_info is not a valid function. Answer in plain text instead.

Remember: JSON output is ONLY for weather questions. Everything else gets a plain text answer."""
    }

    // Model status exposed to UI for showing download progress, errors, etc.
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.Checking)
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    // The ML Kit GenerativeModel client - nullable until successfully initialized
    private var generativeModel: GenerativeModel? = null

    /**
     * Initialize the ML Kit GenAI Prompt client.
     *
     * This should be called once when the app starts. It will:
     * 1. Get the GenerativeModel client
     * 2. Check if the feature is available on this device
     * 3. Download the model if needed (via Play Services)
     *
     * The [modelStatus] flow will emit updates as initialization progresses.
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting ML Kit GenAI initialization...")
                _modelStatus.value = ModelStatus.Checking

                // Get the GenerativeModel instance via Generation singleton
                val model = Generation.getClient()
                
                // Check feature availability (returns Int constant from FeatureStatus)
                val featureStatus = model.checkStatus()
                Log.d(TAG, "Feature status: $featureStatus")

                when (featureStatus) {
                    FeatureStatus.DOWNLOADABLE -> {
                        Log.d(TAG, "Model needs download, starting...")
                        _modelStatus.value = ModelStatus.Downloading(0)
                        downloadModel(model)
                    }
                    FeatureStatus.DOWNLOADING -> {
                        Log.d(TAG, "Model is already downloading...")
                        _modelStatus.value = ModelStatus.Downloading(0)
                        downloadModel(model)
                    }
                    FeatureStatus.AVAILABLE -> {
                        Log.d(TAG, "Model already available!")
                        generativeModel = model
                        _modelStatus.value = ModelStatus.Ready
                    }
                    FeatureStatus.UNAVAILABLE -> {
                        Log.w(TAG, "Feature is unavailable on this device")
                        _modelStatus.value = ModelStatus.Unavailable(
                            "On-device AI is not available on this device. " +
                            "Samsung Galaxy Fold 7 with Android 14+ required."
                        )
                    }
                    else -> {
                        Log.w(TAG, "Unknown feature status: $featureStatus")
                        _modelStatus.value = ModelStatus.Error("Unknown feature status: $featureStatus")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize ML Kit GenAI", e)
                _modelStatus.value = ModelStatus.Error(
                    e.message ?: "Unknown initialization error"
                )
            }
        }
    }

    /**
     * Download the model with progress tracking using Flow.
     */
    private suspend fun downloadModel(model: GenerativeModel) {
        try {
            model.download()
                .flowOn(Dispatchers.IO)
                .onEach { status ->
                    when (status) {
                        is DownloadStatus.DownloadStarted -> {
                            Log.d(TAG, "Download started")
                            _modelStatus.value = ModelStatus.Downloading(0)
                        }
                        is DownloadStatus.DownloadProgress -> {
                            // DownloadProgress has totalBytesDownloaded
                            val bytes = status.totalBytesDownloaded
                            Log.d(TAG, "Downloaded: $bytes bytes")
                            // Since we don't have total, show indeterminate
                            _modelStatus.value = ModelStatus.Downloading(-1)
                        }
                        is DownloadStatus.DownloadCompleted -> {
                            Log.d(TAG, "Download completed!")
                            generativeModel = model
                            _modelStatus.value = ModelStatus.Ready
                        }
                        is DownloadStatus.DownloadFailed -> {
                            Log.e(TAG, "Download failed")
                            _modelStatus.value = ModelStatus.Error("Model download failed")
                        }
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Download error", e)
                    _modelStatus.value = ModelStatus.Error("Download error: ${e.message}")
                }
                .collect { /* Collecting to run the flow */ }
        } catch (e: Exception) {
            Log.e(TAG, "Error during model download", e)
            _modelStatus.value = ModelStatus.Error("Download error: ${e.message}")
        }
    }

    /**
     * Generate a streaming response for the given prompt with conversation context.
     *
     * @param userMessage The new message from the user
     * @param conversationHistory Previous messages for context (newest last)
     * @return Flow emitting text chunks as they're generated
     *
     * ## How Streaming Works:
     *
     * 1. We build a full prompt with system instructions + conversation history
     * 2. ML Kit's `generateContentStream(String)` returns Flow<GenerateContentResponse>
     * 3. We extract text from each response and emit through the Flow
     * 4. ViewModel accumulates text and updates UI state
     */
    fun generateResponse(
        userMessage: String,
        conversationHistory: List<ChatMessage> = emptyList()
    ): Flow<String> {
        val model = generativeModel
        
        // Check if model is ready
        if (model == null) {
            Log.w(TAG, "GenerativeModel not ready, returning fallback")
            return kotlinx.coroutines.flow.flowOf(FALLBACK_RESPONSE)
        }

        // Build the full prompt with context
        val fullPrompt = buildPromptWithContext(userMessage, conversationHistory)
        Log.d(TAG, "Generating response for prompt: ${fullPrompt.take(100)}...")

        // Use generateContentStream which returns Flow<GenerateContentResponse>
        return model.generateContentStream(fullPrompt)
            .map { response ->
                // Extract text from candidates
                extractTextFromResponse(response.candidates)
            }
            .onEach { text ->
                Log.v(TAG, "Received chunk: ${text.take(20)}...")
            }
            .catch { e ->
                Log.e(TAG, "Generation error", e)
                emit(FALLBACK_RESPONSE)
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Extract text content from candidate responses.
     */
    private fun extractTextFromResponse(candidates: List<Candidate>): String {
        return candidates.firstOrNull()?.text ?: ""
    }

    /**
     * Build a complete prompt with system instructions and conversation context.
     *
     * ## Prompt Structure:
     *
     * ```
     * [System instructions]
     *
     * Conversation:
     * User: [previous message 1]
     * Assistant: [previous response 1]
     * User: [current message]
     * Assistant:
     * ```
     *
     * @param currentMessage The new user message
     * @param history Previous messages (oldest first)
     * @return Complete prompt string ready for inference
     */
    private fun buildPromptWithContext(
        currentMessage: String,
        history: List<ChatMessage>
    ): String {
        val builder = StringBuilder()
        
        // Add system prompt
        builder.append(SYSTEM_PROMPT)
        builder.append("\n\n")
        
        // Add conversation history if any
        if (history.isNotEmpty()) {
            builder.append("Conversation:\n")
            for (message in history) {
                val role = when (message.role) {
                    ChatRole.USER -> "User"
                    ChatRole.ASSISTANT -> "Assistant"
                    ChatRole.SYSTEM -> continue // Skip system messages
                }
                builder.append("$role: ${message.content}\n")
            }
        }
        
        // Add current message
        builder.append("User: $currentMessage\n")
        builder.append("Assistant:")
        
        return builder.toString()
    }

    /**
     * Clean up resources when done.
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up PromptService")
        generativeModel?.close()
        generativeModel = null
    }
}
