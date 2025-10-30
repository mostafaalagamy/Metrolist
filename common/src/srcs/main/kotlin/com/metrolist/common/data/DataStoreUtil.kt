package com.metrolist.common.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.metrolist.music.constants.AccountEmailKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUtil @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun setEmail(email: String) {
        dataStore.edit {
            it[AccountEmailKey] = email
        }
    }

    fun getEmail(): Flow<String?> = dataStore.data.map {
        it[AccountEmailKey]
    }
}
