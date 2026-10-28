package com.giathinh.uthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giathinh.uthconnect.di.AuthViewModelFactory
import com.giathinh.uthconnect.feature.auth.AuthViewModel
import com.giathinh.uthconnect.ui.UthConnectApp
import com.giathinh.uthconnect.ui.theme.UthConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UthConnectTheme {
                val factory = remember { AuthViewModelFactory(applicationContext) }
                val authViewModel: AuthViewModel = viewModel(factory = factory)
                UthConnectApp(authViewModel)
            }
        }
    }
}
