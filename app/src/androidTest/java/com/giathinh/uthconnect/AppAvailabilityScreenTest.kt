package com.giathinh.uthconnect

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppAvailabilityScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun maintenanceScreen() = capture(AppAvailability.MAINTENANCE, R.string.maintenance_title, "maintenance.png")
    @Test fun updateScreen() = capture(AppAvailability.UPDATE_REQUIRED, R.string.update_required_title, "update-required.png")

    private fun capture(state: AppAvailability, title: Int, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.setContent { UTHConnectTheme { AvailabilityScreen(state) } }
        rule.onNodeWithText(context.getString(title)).assertExists()
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
