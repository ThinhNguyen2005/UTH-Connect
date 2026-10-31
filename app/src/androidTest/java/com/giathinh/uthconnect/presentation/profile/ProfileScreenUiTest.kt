package com.giathinh.uthconnect.presentation.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenUiTest {
    @get:Rule val rule = createComposeRule()
    private val demo = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")

    @Test fun passwordSettingsCanBeOpenedFromProfile() {
        val actions = mutableListOf<ProfileAction>()
        rule.setContent { UTHConnectTheme { ProfileScreen(ProfileUiState(false, demo), onAction = { actions.add(it) }) } }
        rule.onNodeWithTag("profile_change_password").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(actions.contains(ProfileAction.OpenPassword)) }
    }

    @Test fun logoutRequiresExplicitConfirmation() {
        val actions = mutableListOf<ProfileAction>()
        rule.setContent {
            UTHConnectTheme {
                ProfileScreen(ProfileUiState(false, demo, confirmLogout = true), onAction = { actions.add(it) })
            }
        }
        val title = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.auth_logout_confirm_title)
        rule.onNodeWithText(title).assertExists()
        // No side-effect happens while the dialog is merely displayed.
        rule.runOnIdle { assertTrue(actions.isEmpty()) }
    }
    @Test fun organizerAndVerifiedDemoBadgesAppearOnProfile() {
        val organizer = demo.copy(role = UserRole.ORGANIZER, uthVerified = true)
        rule.setContent {
            UTHConnectTheme {
                ProfileScreen(ProfileUiState(false, organizer), onAction = {})
            }
        }
        rule.onNodeWithTag("profile_verified_badge").assertExists()
        rule.onNodeWithTag("profile_organizer_badge").assertExists()
    }

    @Test fun failedLogoutDialogShowsRetryAction() {
        val actions = mutableListOf<ProfileAction>()
        rule.setContent {
            UTHConnectTheme {
                ProfileScreen(
                    ProfileUiState(false, demo, failure = ProfileFailure.LogoutFailed, confirmLogout = true),
                    onAction = { actions.add(it) },
                )
            }
        }
        rule.onNodeWithTag("logout_retry_error").assertExists()
        rule.onNodeWithTag("logout_retry_or_confirm").performClick()
        rule.runOnIdle { assertTrue(actions.contains(ProfileAction.RetryLogout)) }
    }

}
