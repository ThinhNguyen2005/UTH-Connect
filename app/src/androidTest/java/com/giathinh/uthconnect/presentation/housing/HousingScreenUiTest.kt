package com.giathinh.uthconnect.presentation.housing

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.UthCampus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Luồng chính của feed theo DoD (README mục 10.2): Screen stateless nên không cần Hilt. */
@RunWith(AndroidJUnit4::class)
class HousingScreenUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val listing = HousingListing(
        id = "HS-1",
        type = HousingListingType.ROOM_FOR_RENT,
        campus = UthCampus.CS1_QUANG_TRUNG,
        title = "Phòng gần CS1",
        description = "",
        hostName = "Chủ tin",
        monthlyRentVnd = 2_800_000,
        depositVnd = 0,
        utilitiesIncluded = true,
        availableSlots = 1,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "",
        addressLabel = "Quang Trung",
        latitude = 10.8313,
        longitude = 106.6553,
        distanceToCampusM = 300,
        verified = true,
    )

    private fun string(res: Int, vararg args: Any): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(res, *args)

    private fun setContent(state: HousingUiState, actions: MutableList<HousingAction>) {
        rule.setContent {
            UTHConnectTheme {
                HousingScreen(uiState = state, onAction = { actions.add(it) }, onNavigateBack = {})
            }
        }
    }

    @Test
    fun feedShowsTitlePriceAndVerifiedBadge() {
        setContent(HousingUiState(isLoading = false, listings = listOf(listing)), mutableListOf())

        rule.onNodeWithText(listing.title).assertExists()
        rule.onNodeWithText(string(R.string.housing_price_per_month, "2.800.000")).assertExists()
        rule.onNodeWithText(string(R.string.housing_verified_student_badge)).assertExists()
    }

    @Test
    fun reportButtonEmitsOpenReportWithoutReason() {
        val actions = mutableListOf<HousingAction>()
        setContent(HousingUiState(isLoading = false, listings = listOf(listing)), actions)

        rule.onNodeWithContentDescription(string(R.string.housing_report)).performClick()

        rule.runOnIdle { assertTrue(actions.contains(HousingAction.OpenReport("HS-1"))) }
    }

    @Test
    fun reportDialogDisablesSubmitUntilReasonChosen() {
        val actions = mutableListOf<HousingAction>()
        setContent(
            HousingUiState(isLoading = false, listings = listOf(listing), reportingListingId = "HS-1"),
            actions,
        )

        rule.onNodeWithText(string(R.string.housing_report_submit)).assertIsNotEnabled()
        rule.onNodeWithText(string(R.string.housing_report_reason_fake)).performClick()

        rule.runOnIdle {
            assertTrue(actions.contains(HousingAction.SelectReportReason(HousingReportReason.FAKE_INFO)))
            assertTrue("chưa chọn lý do trong UiState thì chưa được gửi", actions.none { it == HousingAction.ConfirmReport })
        }
    }

    @Test
    fun activeFiltersShowClearAction() {
        val actions = mutableListOf<HousingAction>()
        setContent(
            HousingUiState(
                isLoading = false,
                listings = listOf(listing),
                selectedType = HousingListingType.ROOM_FOR_RENT,
            ),
            actions,
        )

        rule.onNodeWithText(string(R.string.housing_clear_filters)).performClick()

        rule.runOnIdle { assertTrue(actions.contains(HousingAction.ClearFilters)) }
    }

    @Test
    fun filterDialogEmitsRentPreset() {
        val actions = mutableListOf<HousingAction>()
        setContent(HousingUiState(isLoading = false, isFilterDialogOpen = true), actions)

        rule.onNodeWithText(string(R.string.housing_filter_rent_option, "2.500.000")).performClick()

        rule.runOnIdle { assertTrue(actions.contains(HousingAction.SelectMaxRent(2_500_000))) }
    }
}
