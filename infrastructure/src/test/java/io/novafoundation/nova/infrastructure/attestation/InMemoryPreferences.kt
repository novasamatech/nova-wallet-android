package io.novafoundation.nova.infrastructure.attestation

import io.novafoundation.nova.common.data.storage.Editor
import io.novafoundation.nova.common.data.storage.InitialValueProducer
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.core.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class InMemoryPreferences : Preferences {

    private val values = mutableMapOf<String, Any?>()

    override fun contains(field: String) = values.containsKey(field)

    override fun putString(field: String, value: String?) {
        values[field] = value
    }

    override fun getString(field: String, defaultValue: String) = values[field] as? String ?: defaultValue

    override fun getString(field: String) = values[field] as? String

    override fun putBoolean(field: String, value: Boolean) {
        values[field] = value
    }

    override fun getBoolean(field: String, defaultValue: Boolean) = values[field] as? Boolean ?: defaultValue

    override fun putInt(field: String, value: Int) {
        values[field] = value
    }

    override fun putStringSet(field: String, value: Set<String>?) {
        values[field] = value
    }

    override fun getInt(field: String, defaultValue: Int) = values[field] as? Int ?: defaultValue

    override fun putLong(field: String, value: Long) {
        values[field] = value
    }

    override fun getLong(field: String, defaultValue: Long) = values[field] as? Long ?: defaultValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(field: String) = values[field] as? Set<String> ?: emptySet()

    override fun getCurrentLanguage(): Language? = null

    override fun saveCurrentLanguage(languageIsoCode: String) = Unit

    override fun removeField(field: String) {
        values.remove(field)
    }

    override fun stringFlow(field: String, initialValueProducer: InitialValueProducer<String>?): Flow<String?> = emptyFlow()

    override fun booleanFlow(field: String, defaultValue: Boolean): Flow<Boolean> = emptyFlow()

    override fun stringSetFlow(field: String, initialValueProducer: InitialValueProducer<Set<String>>?): Flow<Set<String>?> = emptyFlow()

    override fun keyFlow(key: String): Flow<String> = emptyFlow()

    override fun keysFlow(vararg keys: String): Flow<List<String>> = emptyFlow()

    override fun edit(): Editor = throw UnsupportedOperationException()
}
