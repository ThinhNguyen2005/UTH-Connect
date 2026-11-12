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

private fun draftKey(owner: String) = stringPreferencesKey("create_post_draft_$owner")

/** Nháp đăng tin lưu Preferences DataStore theo tài khoản — không cần mạng/migration. */
@Singleton
class DataStoreHousingDraftStore @Inject constructor(
    @ApplicationContext context: Context,
) : HousingDraftStore {

    private val dataStore = context.housingDraftDataStore

    override suspend fun read(owner: String): String? = dataStore.data.map { it[draftKey(owner)] }.first()

    override suspend fun write(owner: String, payload: String) {
        dataStore.edit { it[draftKey(owner)] = payload }
    }

    override suspend fun clear(owner: String) {
        dataStore.edit { it.remove(draftKey(owner)) }
    }
}
