package io.novafoundation.nova.common.data.storage

import io.novafoundation.nova.core.model.Language
import kotlinx.coroutines.flow.Flow

typealias InitialValueProducer<T> = suspend () -> T

interface Preferences {

    fun contains(field: String): Boolean

    fun putString(field: String, value: String?)

    fun getString(field: String, defaultValue: String): String

    fun getString(field: String): String?

    fun putBoolean(field: String, value: Boolean)

    fun getBoolean(field: String, defaultValue: Boolean): Boolean

    fun putInt(field: String, value: Int)

    fun putStringSet(field: String, value: Set<String>?)

    fun getInt(field: String, defaultValue: Int): Int

    fun putLong(field: String, value: Long)

    fun getLong(field: String, defaultValue: Long): Long

    fun getStringSet(field: String): Set<String>

    fun getCurrentLanguage(): Language?

    fun saveCurrentLanguage(languageIsoCode: String)

    fun removeField(field: String)

    fun stringFlow(
        field: String,
        initialValueProducer: InitialValueProducer<String>? = null
    ): Flow<String?>

    fun booleanFlow(
        field: String,
        defaultValue: Boolean
    ): Flow<Boolean>

    fun stringSetFlow(
        field: String,
        initialValueProducer: InitialValueProducer<Set<String>>? = null
    ): Flow<Set<String>?>

    fun keyFlow(key: String): Flow<String>

    fun keysFlow(vararg keys: String): Flow<List<String>>

    fun edit(): Editor
}

interface Editor {

    fun putString(field: String, value: String?)

    fun putBoolean(field: String, value: Boolean)

    fun putInt(field: String, value: Int)

    fun putLong(field: String, value: Long)

    fun remove(field: String)

    fun apply()
}
