package com.giathinh.uthconnect.presentation.books

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BooksRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRequireSignIn: () -> Unit = onBack,
    viewModel: BooksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BooksScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
        onRequireSignIn = onRequireSignIn,
    )
}
