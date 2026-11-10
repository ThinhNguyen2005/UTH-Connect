package com.giathinh.uthconnect.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

            BooksAction.ResetFilters -> _uiState.update {
                it.copy(
                    query = "",
                    filter = BooksFilter.ALL,
                    condition = null,
                    minimumPrice = "",
                    maximumPrice = "",
                    sort = BooksSort.NEWEST,
                    includeSold = false,
                )
            }

            is BooksAction.Search -> _uiState.update { it.copy(query = action.value) }

            is BooksAction.SetFilter -> _uiState.update { it.copy(filter = action.value) }

            is BooksAction.SetCondition -> _uiState.update { it.copy(condition = action.value) }

            is BooksAction.SetPriceRange -> _uiState.update { it.copy(minimumPrice = action.minimum, maximumPrice = action.maximum) }

            is BooksAction.SetSort -> _uiState.update { it.copy(sort = action.value) }

            is BooksAction.IncludeSold -> _uiState.update { it.copy(includeSold = action.value) }

            is BooksAction.Save -> save(action.listing)

            is BooksAction.SetStatus -> setStatus(action.listing.id, action.status)

            is BooksAction.Delete -> delete(action.id)

            is BooksAction.DismissError -> _uiState.update { it.copy(saveFailure = null) }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailure = null, currentUserUid = listings.currentUserId()) }
            listings.observeListings().catch { throwable ->
                if (throwable is CancellationException) throw throwable
                _uiState.update { state -> state.copy(isLoading = false, loadFailure = BooksFailure.LOAD_FAILED) }
            }.collect { values ->
                // Refreshing listing snapshot must NEVER clear saveFailure
                _uiState.update { it.copy(isLoading = false, listings = values, loadFailure = null) }
            }
        }
    }

    private fun setStatus(id: String, status: BookListingStatus) {
        if (id in _uiState.value.mutatingIds) return
        _uiState.update { it.copy(mutatingIds = it.mutatingIds + id, saveFailure = null) }
        viewModelScope.launch {
            try {
                listings.updateStatus(id, status).fold(
                    onSuccess = {
                        _uiState.update { it.copy(saveFailure = null) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(saveFailure = BooksFailure.UPDATE_STATUS_FAILED) }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailure = BooksFailure.UPDATE_STATUS_FAILED) }
            } finally {
                _uiState.update { it.copy(mutatingIds = it.mutatingIds - id) }
            }
        }
    }

    private fun delete(id: String) {
        if (id in _uiState.value.mutatingIds) return
        _uiState.update { it.copy(mutatingIds = it.mutatingIds + id, saveFailure = null) }
        viewModelScope.launch {
            try {
                listings.delete(id).fold(
                    onSuccess = {
                        _uiState.update { it.copy(saveFailure = null) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(saveFailure = BooksFailure.DELETE_FAILED) }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(saveFailure = BooksFailure.DELETE_FAILED) }
            } finally {
                _uiState.update { it.copy(mutatingIds = it.mutatingIds - id) }
            }
        }
    }

    private fun save(listing: BookListing) {
        _uiState.update { it.copy(isSaving = true, saveFailure = null) }
        viewModelScope.launch {
            try {
                listings.save(listing).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isSaving = false, saveFailure = null, lastSavedId = listing.id) }
                    },
                    onFailure = {
                        _uiState.update { it.copy(isSaving = false, saveFailure = BooksFailure.SAVE_FAILED) }
                    },
                )
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, saveFailure = BooksFailure.SAVE_FAILED) }
            }
        }
    }
}
