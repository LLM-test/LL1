package com.example.hellocompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hellocompose.presentation.ChatScreen
import com.example.hellocompose.presentation.ChatViewModel
import com.example.hellocompose.presentation.expert.ExpertChatScreen
import com.example.hellocompose.presentation.expert.ExpertChatViewModel
import com.example.hellocompose.presentation.modelcomparison.ModelComparisonScreen
import com.example.hellocompose.presentation.modelcomparison.ModelComparisonViewModel
import com.example.hellocompose.presentation.temperature.TemperatureChatScreen
import com.example.hellocompose.presentation.temperature.TemperatureChatViewModel
import com.example.hellocompose.ui.theme.HelloComposeTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val chatViewModel: ChatViewModel by viewModel()
    private val expertChatViewModel: ExpertChatViewModel by viewModel()
    private val temperatureChatViewModel: TemperatureChatViewModel by viewModel()
    private val modelComparisonViewModel: ModelComparisonViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "chat"
                    ) {
                        composable("chat") {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateToExperts = {
                                    navController.navigate("experts")
                                },
                                onNavigateToTemperature = {
                                    navController.navigate("temperature")
                                },
                                onNavigateToModelComparison = {
                                    navController.navigate("model_comparison")
                                }
                            )
                        }
                        composable("experts") {
                            ExpertChatScreen(
                                viewModel = expertChatViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("temperature") {
                            TemperatureChatScreen(
                                viewModel = temperatureChatViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("model_comparison") {
                            ModelComparisonScreen(
                                viewModel = modelComparisonViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
