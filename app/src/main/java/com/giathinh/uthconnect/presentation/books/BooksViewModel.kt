package com.giathinh.uthconnect.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val listings: BookListingUseCases,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        observe()
    }

    fun onAction(action: BooksAction) {
        when (action) {
            BooksAction.Retry -> observe()
            is BooksAction.Search -> _uiState.update { it.copy(query = action.value) }
            is BooksAction.SetFilter -> _uiState.update { it.copy(filter = action.value) }
            is BooksAction.SetCondition -> _uiState.update { it.copy(condition = action.value) }
            is BooksAction.SetPriceRange -> _uiState.update { it.copy(minimumPrice = action.minimum, maximumPrice = action.maximum) }
            is BooksAction.SetSort -> _uiState.update { it.copy(sort = action.value) }
            is BooksAction.IncludeSold -> _uiState.update { it.copy(includeSold = action.value) }
            is BooksAction.Save -> save(action.listing)
            is BooksAction.SetStatus -> mutate { listings.save(action.listing.copy(status = action.status)) }
            is BooksAction.Delete -> mutate { listings.delete(action.id) }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null, currentUserUid = listings.currentUserId()) }
            listings.observeListings().catch {
                _uiState.update { state -> state.copy(isLoading = false, failure = BooksFailure.LOAD_FAILED) }
            }.collect { values ->
                _uiState.update { it.copy(isLoading = false, listings = values, failure = null) }
            }
        }
    }

    private fun mutate(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().fold(
                onSuccess = { _uiState.update { it.copy(failure = null) } },
                onFailure = { _uiState.update { it.copy(failure = BooksFailure.SAVE_FAILED) } },
            )
        }
    }

    private fun save(listing: BookListing) {
        _uiState.update { it.copy(isSaving = true, failure = null) }
        viewModelScope.launch {
            listings.save(listing).fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, failure = null) } },
                onFailure = { _uiState.update { it.copy(isSaving = false, failure = BooksFailure.SAVE_FAILED) } },
            )
        }
    }
}
