package com.giathinh.uthconnect.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.giathinh.uthconnect.data.auth.DemoAuthRepository
import com.giathinh.uthconnect.data.session.DataStoreSessionRepository
import com.giathinh.uthconnect.domain.usecase.LoginUseCase
import com.giathinh.uthconnect.feature.auth.AuthViewModel

/** Constructor injection for this focused module; switch to Hilt on repo integration. */
class AuthViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java))
        val repository = DemoAuthRepository()
        val session = DataStoreSessionRepository(applicationContext)
        return AuthViewModel(LoginUseCase(repository), repository, session) as T
    }
}
