package com.azime.aitestapp

import java.util.UUID

/**
 * Represents a single message in the chat conversation.
 *
 * @property id Unique identifier for this message
 * @property content The text content of the message
 * @property role The role of who sent this message (User or Assistant)
 * @property timestamp When the message was created (epoch millis)
 * @property isStreaming Whether this message is still receiving streaming tokens
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: ChatRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

/**
 * Defines the role of a message sender.
 */
enum class ChatRole {
    /** Message from the human user */
    USER,
    /** Message from the AI assistant */
    ASSISTANT,
    /** System instruction (not displayed in UI) */
    SYSTEM
}

/**
 * Represents the current status of the on-device ML model.
 *
 * The model goes through these states:
 * 1. Checking -> Is the feature available on this device?
 * 2. Downloading -> Model is being downloaded via Play Services
 * 3. Ready -> Model is loaded and ready for inference
 * 4. Error -> Something went wrong (with message)
 * 5. Unavailable -> Device doesn't support on-device AI
 */
sealed class ModelStatus {
    /** Checking if feature is available on device */
    data object Checking : ModelStatus()
    
    /** Model is being downloaded */
    data class Downloading(val progress: Int = 0) : ModelStatus()
    
    /** Model is ready for inference */
    data object Ready : ModelStatus()
    
    /** An error occurred */
    data class Error(val message: String) : ModelStatus()
    
    /** Feature not available on this device */
    data class Unavailable(val reason: String) : ModelStatus()
}

/**
 * Represents the UI state for the chat screen.
 */
sealed class ChatUiState {
    /** Chat is idle, waiting for user input */
    data object Idle : ChatUiState()
    
    /** AI is generating a response with streaming tokens */
    data class Generating(val partialResponse: String = "") : ChatUiState()
    
    /** An error occurred during generation */
    data class Error(val message: String) : ChatUiState()
}
