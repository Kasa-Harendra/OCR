package com.example.ondevicerag.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ondevicerag.ui.screens.CameraCaptureScreen
import com.example.ondevicerag.ui.screens.DocumentListScreen
import com.example.ondevicerag.ui.screens.ExtractedTextViewerScreen
import com.example.ondevicerag.ui.screens.HomeScreen
import com.example.ondevicerag.ui.screens.IngestionScreen
import com.example.ondevicerag.ui.screens.RagChatScreen
import com.example.ondevicerag.ui.theme.DarkBackground
import com.example.ondevicerag.ui.theme.OnDeviceRAGTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermission()

        setContent {
            OnDeviceRAGTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToIngest = { navController.navigate("ingest") },
                onNavigateToCamera = { navController.navigate("camera") },
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToDocs = { navController.navigate("docs") },
                onNavigateToTextViewer = { docId -> navController.navigate("text_viewer/$docId") }
            )
        }
        composable("ingest") {
            IngestionScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("camera") {
            CameraCaptureScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCaptured = { navController.navigate("ingest") }
            )
        }
        composable("chat") {
            RagChatScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("docs") {
            DocumentListScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onSelectDocument = { docId -> navController.navigate("text_viewer/$docId") }
            )
        }
        composable(
            route = "text_viewer/{docId}",
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId")
            ExtractedTextViewerScreen(
                viewModel = viewModel,
                docId = docId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
