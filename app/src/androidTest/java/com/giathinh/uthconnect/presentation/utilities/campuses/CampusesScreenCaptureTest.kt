package com.giathinh.uthconnect.presentation.utilities.campuses

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusesScreenCaptureTest {
    @get:Rule val rule = createComposeRule()

    @Test fun campusOneAddressAndDirectionsAreVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val address = CampusesUiState().campuses.first().address
        rule.setContent { UTHConnectTheme { CampusesScreen(onBack = {}) } }

        rule.onNodeWithText(address).assertExists()
        rule.onAllNodesWithText(context.getString(R.string.campuses_directions_action)).assertCountEquals(3)

        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(context.getExternalFilesDir(null), "uth-campus-screen.png")
        file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
