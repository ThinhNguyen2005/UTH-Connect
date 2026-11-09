package com.giathinh.uthconnect

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteConfigNetworkTest {
    @get:Rule val rule = createComposeRule()

    @Test fun fetchAndActivate() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("remote_config_network") == "true")
        val config = FirebaseRemoteConfig.getInstance()
        Tasks.await(config.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()), 30, TimeUnit.SECONDS)
        Tasks.await(config.setDefaultsAsync(mapOf("force_update_min_version" to "1.0.0", "maintenance_mode" to false)), 30, TimeUnit.SECONDS)
        Tasks.await(config.fetchAndActivate(), 60, TimeUnit.SECONDS)
        assertEquals(FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS, config.info.lastFetchStatus)
        val minimum = config.getString("force_update_min_version")
        val maintenance = config.getBoolean("maintenance_mode")
        fun source(key: String) = when (config.getValue(key).source) {
            FirebaseRemoteConfig.VALUE_SOURCE_REMOTE -> "remote"
            FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT -> "in-app default"
            else -> "static"
        }
        val minimumSource = source("force_update_min_version")
        val maintenanceSource = source("maintenance_mode")
        rule.setContent {
            Column(Modifier.padding(24.dp)) {
                Text("Remote Config: fetch & activate OK")
                Text("force_update_min_version: $minimum ($minimumSource)")
                Text("maintenance_mode: $maintenance ($maintenanceSource)")
            }
        }
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "remote-config-fetch.png").outputStream().use {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
