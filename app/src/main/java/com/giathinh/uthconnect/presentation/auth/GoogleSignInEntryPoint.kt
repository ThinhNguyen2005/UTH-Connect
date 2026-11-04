package com.giathinh.uthconnect.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.giathinh.uthconnect.data.repository.GoogleIdTokenSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Credential Manager is platform UI, so — like Android BiometricPrompt — it is driven from the
 * route and never from [AuthViewModel]. Hilt has no injection point inside a plain composable,
 * hence this entry point: the route reaches the singleton without the ViewModel ever holding an
 * Android dependency.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface GoogleSignInEntryPoint {
    fun idTokenSource(): GoogleIdTokenSource
}

@Composable
fun rememberGoogleIdTokenSource(): GoogleIdTokenSource {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            GoogleSignInEntryPoint::class.java,
        ).idTokenSource()
    }
}
