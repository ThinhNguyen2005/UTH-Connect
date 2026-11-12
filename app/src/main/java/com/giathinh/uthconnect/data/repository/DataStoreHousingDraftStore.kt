package com.giathinh.uthconnect.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giathinh.uthconnect.domain.repository.HousingDraftStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.housingDraftDataStore by preferencesDataStore(name = "housing_drafts")

private val DRAFT_KEY = stringPreferencesKey("create_post_draft")

/** Nháp đăng tin lưu Preferences DataStore trong bộ nhớ ứng dụng — không cần mạng/migration. */
@Singleton
class DataStoreHousingDraftStore @Inject constructor(
    @ApplicationContext context: Context,
) : HousingDraftStore {

    private val dataStore = context.housingDraftDataStore

    override suspend fun read(): String? = dataStore.data.map { it[DRAFT_KEY] }.first()

    override suspend fun write(payload: String) {
        dataStore.edit { it[DRAFT_KEY] = payload }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(DRAFT_KEY) }
    }
}
