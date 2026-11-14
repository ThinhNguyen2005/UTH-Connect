package com.giathinh.uthconnect.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giathinh.uthconnect.domain.repository.HousingImageCleanupStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.housingImageCleanupDataStore by preferencesDataStore(name = "housing_image_cleanup")

private val PENDING_KEY = stringSetPreferencesKey("pending_paths")

/** Hàng đợi ảnh mồ côi lưu Preferences DataStore trong bộ nhớ ứng dụng — không cần mạng/migration. */
@Singleton
class DataStoreHousingImageCleanupStore @Inject constructor(
    @ApplicationContext context: Context,
) : HousingImageCleanupStore {

    private val dataStore = context.housingImageCleanupDataStore

    override suspend fun pending(): List<String> = dataStore.data.map { it[PENDING_KEY].orEmpty() }.first().toList()

    override suspend fun enqueue(paths: List<String>) {
        if (paths.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[PENDING_KEY] = prefs[PENDING_KEY].orEmpty() + paths
        }
    }

    override suspend fun dequeue(paths: List<String>) {
        if (paths.isEmpty()) return
        dataStore.edit { prefs ->
            prefs[PENDING_KEY] = prefs[PENDING_KEY].orEmpty() - paths.toSet()
        }
    }
}
