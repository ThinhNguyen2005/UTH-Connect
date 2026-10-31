package com.giathinh.uthconnect.presentation.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.AppSettings
import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.usecase.GetSettingsUseCase
import com.giathinh.uthconnect.domain.usecase.GetTrainingPointUseCase
import com.giathinh.uthconnect.domain.usecase.GetTuitionUseCase
import com.giathinh.uthconnect.domain.usecase.GetUtilityModulesUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateSettingsUseCase
import com.giathinh.uthconnect.presentation.utilities.settings.SettingsUiState
import com.giathinh.uthconnect.presentation.utilities.trainingpoint.TrainingCriteria
import com.giathinh.uthconnect.presentation.utilities.trainingpoint.TrainingPointUiState
import com.giathinh.uthconnect.presentation.utilities.tuition.TuitionCourse
import com.giathinh.uthconnect.presentation.utilities.tuition.TuitionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val getUtilityModules: GetUtilityModulesUseCase,
    private val getTuition: GetTuitionUseCase,
    private val getTrainingPoint: GetTrainingPointUseCase,
    getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UtilitiesUiState())
    val uiState: StateFlow<UtilitiesUiState> = _uiState.asStateFlow()

    private val _tuitionState = MutableStateFlow(TuitionUiState())
    val tuitionState: StateFlow<TuitionUiState> = _tuitionState.asStateFlow()

    private val _trainingPointState = MutableStateFlow(TrainingPointUiState())
    val trainingPointState: StateFlow<TrainingPointUiState> = _trainingPointState.asStateFlow()

    val settingsState: StateFlow<SettingsUiState> = getSettings()
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    init {
        loadModules()
        loadTuition()
        loadTrainingPoint()
    }

    fun onAction(action: UtilitiesAction) {
        when (action) {
            UtilitiesAction.Retry -> {
                loadModules()
                loadTuition()
                loadTrainingPoint()
            }
            is UtilitiesAction.OpenModule -> {
                // Handled via navigation routes
            }
        }
    }

    fun onToggleTuitionReminder(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setTuitionReminder(enabled)
        }
    }

    fun onToggleTrainingPointNotif(enabled: Boolean) {
        viewModelScope.launch {
            updateSettings.setTrainingPointNotif(enabled)
        }
    }

    private fun loadModules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, failure = null) }
            getUtilityModules().fold(
                onSuccess = { modules ->
                    _uiState.update { it.copy(isLoading = false, modules = modules, failure = null) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(isLoading = false, modules = emptyList(), failure = UtilitiesFailure.LoadFailed)
                    }
                },
            )
        }
    }

    private fun loadTuition() {
        viewModelScope.launch {
            getTuition().onSuccess { info ->
                _tuitionState.value = info.toUiState()
            }
        }
    }

    private fun loadTrainingPoint() {
        viewModelScope.launch {
            getTrainingPoint().onSuccess { info ->
                _trainingPointState.value = info.toUiState()
            }
        }
    }

    private fun TuitionInfo.toUiState() = TuitionUiState(
        semester = semester,
        isPaid = isPaid,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        creditsCount = creditsCount,
        dueDate = dueDate,
        courses = courses.map { TuitionCourse(it.code, it.name, it.credits, it.fee) },
    )

    private fun TrainingPointInfo.toUiState() = TrainingPointUiState(
        semester = semester,
        totalScore = totalScore,
        maxScore = maxScore,
        rank = rank,
        criteriaList = criteriaList.map { TrainingCriteria(it.title, it.score, it.maxScore, it.description) },
    )

    private fun AppSettings.toUiState() = SettingsUiState(
        tuitionReminderEnabled = tuitionReminderEnabled,
        trainingPointNotifEnabled = trainingPointNotifEnabled,
        appVersion = appVersion,
        buildNumber = buildNumber,
        developer = developer,
    )
}
