package com.giathinh.uthconnect.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import com.giathinh.uthconnect.domain.usecase.GetHomeFeedUseCase
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

/**
 * Giữ toàn bộ trạng thái của tab Trang chủ theo mô hình UDF:
 *
 * ```
 * UiState ──▶ Screen ──▶ Action ──▶ ViewModel ──▶ UseCase ──▶ Repository
 * ```
 *
 * - Không `Context`/`Modifier`/`@StringRes`; `_uiState` private, chỉ expose `StateFlow`.
 * - Không giữ danh sách gốc ở biến ngoài `UiState`: bộ lọc là `selectedCategory` và `selectedCampus`, danh sách
 *   hiển thị suy ra bằng `visibleAnnouncements` và `visibleEvents` → chỉ một nguồn sự thật.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val eventRepository: EventRepository,
    private val housingRepository: HousingRepository? = null,
    private val bookListingRepository: BookListingRepository? = null,
    private val lostFoundRepository: LostFoundRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var feedJob: Job? = null
    private var eventsJob: Job? = null
    private var registrationsJob: Job? = null
    private var housingJob: Job? = null
    private var booksJob: Job? = null
    private var lostFoundJob: Job? = null

    init {
        load()
    }

    /** Điểm vào duy nhất: Screen chỉ phát action, không tự nạp lại. */
    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Retry -> load()

            is HomeAction.SelectCategory -> _uiState.update { it.copy(selectedCategory = action.category) }

            is HomeAction.SelectCampus -> _uiState.update { it.copy(selectedCampus = action.campus) }

            // Các action điều hướng dưới đây sẽ do HomeRoute chặn để xử lý điều hướng,
            // ViewModel chỉ giữ nhánh để `when` đầy đủ.
            HomeAction.OpenNotifications,
            HomeAction.OpenFilter,
            HomeAction.OpenArchive,
            HomeAction.OpenAllEvents,
            is HomeAction.OpenAnnouncement,
            is HomeAction.OpenEvent,
            is HomeAction.OpenHousingDetail,
            HomeAction.OpenAllHousing,
            is HomeAction.OpenBookDetail,
            HomeAction.OpenAllBooks,
            is HomeAction.OpenLostFoundDetail,
            HomeAction.OpenAllLostFound,
            -> Unit
        }
    }

    private fun cancelJobs() {
        feedJob?.cancel()
        eventsJob?.cancel()
        registrationsJob?.cancel()
        housingJob?.cancel()
        booksJob?.cancel()
        lostFoundJob?.cancel()
    }

    private fun load() {
        cancelJobs()
        _uiState.update { it.copy(isLoading = true, failure = null) }
        loadFeed()
        loadEvents()
        loadCommunity()
        eventRepository.refresh()
        bookListingRepository?.refresh()
    }

    private fun loadFeed() {
        feedJob = viewModelScope.launch {
            getHomeFeed().fold(
                onSuccess = { feed ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            banners = feed.banners,
                            announcements = feed.latest,
                            failure = null,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            banners = emptyList(),
                            announcements = emptyList(),
                            failure = HomeFailure.LoadFailed,
                        )
                    }
                },
            )
        }
    }

    private fun loadEvents() {
        eventsJob = viewModelScope.launch {
            eventRepository.getEvents()
                .catch { error ->
                    if (error is CancellationException) throw error
                }
                .collect { eventList ->
                    _uiState.update { it.copy(events = eventList) }
                }
        }

        registrationsJob = viewModelScope.launch {
            eventRepository.currentUserRegistrations()
                .catch { error ->
                    if (error is CancellationException) throw error
                }
                .collect { ids ->
                    _uiState.update { it.copy(registeredEventIds = ids) }
                }
        }
    }

    private fun loadCommunity() {
        housingJob = viewModelScope.launch {
            housingRepository?.getListings()?.fold(
                onSuccess = { listings ->
                    _uiState.update { it.copy(housingListings = listings) }
                },
                onFailure = {
                    // Non-fatal: tiện ích bổ trợ không làm hỏng toàn bộ màn hình chính
                },
            )
        }

        booksJob = viewModelScope.launch {
            bookListingRepository?.observeListings()
                ?.catch { error ->
                    if (error is CancellationException) throw error
                }
                ?.collect { listings ->
                    _uiState.update { it.copy(bookListings = listings) }
                }
        }

        lostFoundJob = viewModelScope.launch {
            lostFoundRepository?.getListings()?.fold(
                onSuccess = { listings ->
                    _uiState.update { it.copy(lostFoundListings = listings) }
                },
                onFailure = {
                    // Non-fatal
                },
            )
        }
    }
}
