package com.azime.aitestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.azime.aitestapp.ui.theme.AITestAppTheme

/**
 * Main Activity for the On-Device LLM Chat App.
 *
 * ## Architecture Overview:
 *
 * This activity follows a clean architecture approach:
 *
 * ```
 * MainActivity
 *     ├── Creates PromptService (singleton for model management)
 *     ├── Creates ChatViewModel (via ViewModelProvider)
 *     └── Sets Compose content
 *            └── ChatScreen (observes ViewModel state)
 * ```
 *
 * ## Lifecycle Considerations:
 *
 * - PromptService is tied to Activity lifecycle (lazy init)
 * - ChatViewModel survives configuration changes
 * - Model stays loaded in memory across rotations
 *
 * ## Why Lazy PromptService?
 *
 * The ML Kit model is expensive to load (~500MB-1GB RAM). By using lazy
 * initialization, we delay loading until actually needed and ensure we
 * only have one instance.
 */
class MainActivity : ComponentActivity() {

    // Lazy initialization of the prompt service
    // This delays model loading until first access
    private val promptService by lazy { 
        PromptService() 
    }

    // ViewModel with custom factory to inject PromptService
    private val viewModel: ChatViewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                ChatViewModel(application, promptService)
            }
        }
        ViewModelProvider(this, factory)[ChatViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            AITestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}