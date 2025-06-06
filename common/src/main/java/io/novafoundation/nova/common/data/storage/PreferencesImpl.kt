package io.novafoundation.nova.common.data.storage

import android.content.SharedPreferences
import io.novafoundation.nova.core.model.Language
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

class PreferencesImpl(
    private val sharedPreferences: SharedPreferences
) : Preferences {

    /*
    SharedPreferencesImpl stores listeners in a WeakHashMap,
    meaning listener is subject to GC if it is not kept anywhere else.
    This is not a problem until a stringFlow() call is followed later by shareIn() or stateIn(),
    which cause listener to be GC-ed (TODO - research why).
    To avoid that, store strong references to listeners until corresponding flow is closed.
    */
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    companion object {

        private const val PREFS_SELECTED_LANGUAGE = "selected_language"
    }

    override fun contains(field: String) = sharedPreferences.contains(field)

    override fun putString(field: String, value: String?) {
        sharedPreferences.edit().putString(field, value).apply()
    }

    override fun getString(field: String, defaultValue: String): String {
        return sharedPreferences.getString(field, defaultValue) ?: defaultValue
    }

    override fun getString(field: String): String? {
        return sharedPreferences.getString(field, null)
    }

    override fun putBoolean(field: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(field, value).apply()
    }

    override fun getBoolean(field: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(field, defaultValue)
    }

    override fun putInt(field: String, value: Int) {
        sharedPreferences.edit().putInt(field, value).apply()
    }

    override fun getInt(field: String, defaultValue: Int): Int {
        return sharedPreferences.getInt(field, defaultValue)
    }

    override fun putLong(field: String, value: Long) {
        sharedPreferences.edit().putLong(field, value).apply()
    }

    override fun putStringSet(field: String, value: Set<String>?) {
        sharedPreferences.edit().putStringSet(field, value).apply()
    }

    override fun getLong(field: String, defaultValue: Long): Long {
        return sharedPreferences.getLong(field, defaultValue)
    }

    override fun getStringSet(field: String): Set<String> {
        return sharedPreferences.getStringSet(field, emptySet()) ?: emptySet()
    }

    override fun getCurrentLanguage(): Language? {
        return if (sharedPreferences.contains(PREFS_SELECTED_LANGUAGE)) {
            Language(iso639Code = sharedPreferences.getString(PREFS_SELECTED_LANGUAGE, "")!!)
        } else {
            null
        }
    }

    override fun saveCurrentLanguage(languageIsoCode: String) {
        sharedPreferences.edit().putString(PREFS_SELECTED_LANGUAGE, languageIsoCode).apply()
    }

    override fun removeField(field: String) {
        sharedPreferences.edit().remove(field).apply()
    }

    override fun stringFlow(
        field: String,
        initialValueProducer: (suspend () -> String)?
    ): Flow<String?> = keyFlow(field)
        .map {
            if (contains(field)) {
                getString(field)
            } else {
                val initialValue = initialValueProducer?.invoke()
                putString(field, initialValue)
                initialValue
            }
        }

    override fun booleanFlow(field: String, defaultValue: Boolean): Flow<Boolean> {
        return keyFlow(field).map {
            getBoolean(field, defaultValue)
        }
    }

    override fun stringSetFlow(field: String, initialValueProducer: InitialValueProducer<Set<String>>?): Flow<Set<String>?> {
        return keyFlow(field).map {
            if (contains(field)) {
                getStringSet(field)
            } else {
                val initialValue = initialValueProducer?.invoke()
                putStringSet(field, initialValue)
                initialValue
            }
        }
    }

    override fun keyFlow(key: String): Flow<String> = keysFlow(key)
        .map { it.first() }

    override fun keysFlow(vararg keys: String): Flow<List<String>> = callbackFlow {
        send(keys.toList())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key in keys) {
                trySend(listOfNotNull(key))
            }
        }

        listeners.add(listener)
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            listeners.remove(listener)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun edit(): Editor {
        return SharedPreferenceEditor(sharedPreferences)
    }
}
