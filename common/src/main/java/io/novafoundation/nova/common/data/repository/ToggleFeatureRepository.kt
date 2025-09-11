package io.novafoundation.nova.common.data.repository

import io.novafoundation.nova.common.data.storage.Preferences
import kotlinx.coroutines.flow.Flow

interface ToggleFeatureRepository {
    fun get(key: String, default: Boolean = false): Boolean

    fun set(key: String, value: Boolean)

    fun observe(key: String, default: Boolean = false): Flow<Boolean>
}

class RealToggleFeatureRepository(
    private val preferences: Preferences
) : ToggleFeatureRepository {

    override fun get(key: String, default: Boolean): Boolean {
        return preferences.getBoolean(key, default)
    }

    override fun set(key: String, value: Boolean) {
        preferences.putBoolean(key, value)
    }

    override fun observe(key: String, default: Boolean): Flow<Boolean> {
        return preferences.booleanFlow(key, default)
    }
}

fun ToggleFeatureRepository.toggle(key: String) {
    set(key, !get(key))
}
