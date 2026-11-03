package com.giathinh.uthconnect.presentation.auth

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.presentation.profile.ProfileAction
import com.giathinh.uthconnect.presentation.profile.ProfileScreen
import com.giathinh.uthconnect.presentation.profile.ProfileUiState
import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Đổi locale chỉ trong composition test, không sửa cài đặt thiết bị của người dùng. */
@RunWith(AndroidJUnit4::class)
class AuthProfileLocaleUiTest {
    @get:Rule val rule = createComposeRule()

    private fun checkScreen(locale: String, profile: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(locale)) }
        val localized = context.createConfigurationContext(configuration)
        var clicked = false
        rule.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                UTHConnectTheme {
                    if (profile) ProfileScreen(ProfileUiState(isLoading = false,
                        student = StudentProfile("2023040123", "student@ut.edu.vn", "Test Student", "Class", "Major", "Faculty")),
                        onAction = { if (it == ProfileAction.OpenContact) clicked = true })
                    else AuthScreen(AuthUiState(isRestoring = false),
                        onAction = { if (it == AuthAction.Submit) clicked = true })
                }
            }
        }
        rule.onNodeWithText(localized.getString(if (profile) R.string.auth_profile_title else R.string.auth_title)).assertExists()
        rule.onNodeWithTag(if (profile) "profile_edit_contact" else "auth_sign_in").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(clicked) }
    }

    @Test fun authVietnameseAtLargeFont() = checkScreen("vi", false)
    @Test fun authEnglishAtLargeFont() = checkScreen("en", false)
    @Test fun profileVietnameseAtLargeFont() = checkScreen("vi", true)
    @Test fun profileEnglishAtLargeFont() = checkScreen("en", true)
}
