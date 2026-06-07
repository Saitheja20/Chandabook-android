package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.ChandaBookAppContent
import com.example.ui.ChandaBookViewModel
import com.example.ui.ChandaBookViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Retrieve AppContainer dependencies from ChandaBookApplication
        val app = application as ChandaBookApplication
        val container = app.container

        val viewModelFactory = ChandaBookViewModelFactory(
            application = app,
            authRepo = container.authRepository,
            orgRepo = container.orgRepository,
            donationRepo = container.donationRepository,
            expenseRepo = container.expenseRepository
        )

        // Instantiate unified screen state manager
        val viewModel = ViewModelProvider(this, viewModelFactory)[ChandaBookViewModel::class.java]

        // Handle incoming target deep links from notification taps
        intent?.getStringExtra("FCM_TARGET_SCREEN")?.let { targetScreen ->
            viewModel.setFCMNavigationRoute(targetScreen)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ChandaBookAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("FCM_TARGET_SCREEN")?.let { targetScreen ->
            try {
                val app = application as ChandaBookApplication
                val container = app.container
                val viewModelFactory = ChandaBookViewModelFactory(
                    application = app,
                    authRepo = container.authRepository,
                    orgRepo = container.orgRepository,
                    donationRepo = container.donationRepository,
                    expenseRepo = container.expenseRepository
                )
                val viewModel = ViewModelProvider(this, viewModelFactory)[ChandaBookViewModel::class.java]
                viewModel.setFCMNavigationRoute(targetScreen)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
