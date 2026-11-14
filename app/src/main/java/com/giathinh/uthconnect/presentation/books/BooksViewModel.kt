package com.giathinh.uthconnect.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.BookListingUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val listings: BookListingUseCases,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    constructor(listings: BookListingUseCases) : this(listings, NoOpSessionRepository)

    private val _uiState = MutableStateFlow(BooksUiState())
    val uiState = _uiState.asStateFlow()
    private var observation: Job? = null

    init {
        observeSession()
        observe()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.session.collect { currentSession ->
                val profile = currentSession.profile
                val currentUid = listings.currentUserId() ?: profile?.studentId
                _uiState.update { state ->
                    state.copy(
                        currentProfile = profile,
                        currentUserUid = currentUid,
                    )
                }
            }
        }
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

            is BooksAction.SetPriceRange -> _uiState.update {
                it.copy(minimumPrice = action.minimum, maximumPrice = action.maximum)
            }

            is BooksAction.SetSort -> _uiState.update { it.copy(sort = action.value) }

            is BooksAction.IncludeSold -> _uiState.update { it.copy(includeSold = action.value) }

            is BooksAction.Save -> save(action.listing)

            is BooksAction.SetStatus -> setStatus(action.listing.id, action.status)

            is BooksAction.Delete -> delete(action.id)

            is BooksAction.DismissError -> _uiState.update {
                it.copy(saveFailure = null, saveStep = BookSaveStep.IDLE)
            }
        }
    }

    private fun observe() {
        observation?.cancel()
        observation = viewModelScope.launch {
            val userUid = listings.currentUserId() ?: _uiState.value.currentProfile?.studentId
            _uiState.update { it.copy(isLoading = true, loadFailure = null, currentUserUid = userUid) }
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
        val initialStep = if (listing.photoUri != null) {
            BookSaveStep.UPLOADING_IMAGE
        } else {
            BookSaveStep.SAVING_LISTING
        }
        _uiState.update { it.copy(isSaving = true, saveStep = initialStep, saveFailure = null) }
        viewModelScope.launch {
            try {
                if (listing.photoUri != null) {
                    _uiState.update { it.copy(saveStep = BookSaveStep.SAVING_LISTING) }
                }
                listings.save(listing).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveStep = BookSaveStep.SUCCESS,
                                saveFailure = null,
                                lastSavedId = listing.id,
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveStep = BookSaveStep.IDLE,
                                saveFailure = BooksFailure.SAVE_FAILED,
                            )
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSaving = false, saveStep = BookSaveStep.IDLE) }
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveStep = BookSaveStep.IDLE,
                        saveFailure = BooksFailure.SAVE_FAILED,
                    )
                }
            }
        }
    }
}

private object NoOpSessionRepository : SessionRepository {
    private val stub = MutableStateFlow(StudentSession(isRestoring = false, profile = null))
    override val session: StateFlow<StudentSession> = stub
    override suspend fun restore() = Unit
    override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
    override suspend fun updateProfile(profile: StudentProfile) = Unit
    override suspend fun enableBiometric(studentId: String) = Unit
    override suspend fun disableBiometric() = Unit
    override suspend fun signOut() = Unit
}
