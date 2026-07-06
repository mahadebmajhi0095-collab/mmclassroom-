package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LiveClassScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecordedClassesScreen
import com.example.ui.screens.StudentProgressScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ClassroomViewModel
import com.example.notifications.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ClassroomViewModel = viewModel()
                    val user by viewModel.userState.collectAsState()

                    // State-driven navigation router (avoiding navigation library runtime errors)
                    var currentScreen by remember { mutableStateOf("LOGIN") }

                    // Automatic redirect to Dashboard if user profile already exists
                    LaunchedEffect(user) {
                        if (user != null) {
                            if (currentScreen == "LOGIN") {
                                currentScreen = "DASHBOARD"
                            }
                        } else {
                            currentScreen = "LOGIN"
                        }
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_navigation"
                    ) { screen ->
                        when (screen) {
                            "LOGIN" -> {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onLoginSuccess = {
                                        currentScreen = "DASHBOARD"
                                    }
                                )
                            }
                            "DASHBOARD" -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onEnterLiveClass = {
                                        currentScreen = "LIVE_CLASS"
                                    },
                                    onEnterRecordedClasses = {
                                        currentScreen = "RECORDED_CLASSES"
                                    },
                                    onEnterStudentProgress = {
                                        currentScreen = "STUDENT_PROGRESS"
                                    },
                                    onSignOut = {
                                        // Simple simulated clear
                                        viewModel.exitActiveClassroom()
                                        // Reset state-driven navigation to allow login re-entry
                                        currentScreen = "LOGIN"
                                    }
                                )
                            }
                            "LIVE_CLASS" -> {
                                LiveClassScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.exitActiveClassroom()
                                        currentScreen = "DASHBOARD"
                                    }
                                )
                            }
                            "RECORDED_CLASSES" -> {
                                RecordedClassesScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        viewModel.exitActiveClassroom()
                                        currentScreen = "DASHBOARD"
                                    }
                                )
                            }
                            "STUDENT_PROGRESS" -> {
                                StudentProgressScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        currentScreen = "DASHBOARD"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
