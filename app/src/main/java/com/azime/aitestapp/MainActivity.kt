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
import com.azime.aitestapp.tools.ToolRegistry
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
 *     ├── Creates ToolRegistry (for agentic tool invocations)
 *     ├── Creates ChatViewModel (via ViewModelProvider)
 *     └── Sets Compose content
 *            └── ChatScreen (observes ViewModel state)
 * ```
 *
 * ## Lifecycle Considerations:
 *
 * - PromptService is tied to Activity lifecycle (lazy init)
 * - ToolRegistry requires Context for device APIs (battery, etc.)
 * - ChatViewModel survives configuration changes
 * - Model stays loaded in memory across rotations
 */
class MainActivity : ComponentActivity() {

    // Lazy initialization of the prompt service
    private val promptService by lazy { 
        PromptService() 
    }

    // Lazy initialization of the tool registry (for battery, etc.)
    private val toolRegistry by lazy {
        ToolRegistry(applicationContext)
    }

    // ViewModel with custom factory to inject PromptService and ToolRegistry
    private val viewModel: ChatViewModel by lazy {
        val factory = viewModelFactory {
            initializer {
                ChatViewModel(application, promptService, toolRegistry)
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