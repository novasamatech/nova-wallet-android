package io.novafoundation.nova.common.data.storage

import android.content.SharedPreferences

class SharedPreferenceEditor(private val sharedPreferences: SharedPreferences) : Editor {

    private val editor = sharedPreferences.edit()

    override fun putString(field: String, value: String?) {
        editor.putString(field, value)
    }

    override fun putBoolean(field: String, value: Boolean) {
        editor.putBoolean(field, value)
    }

    override fun putInt(field: String, value: Int) {
        editor.putInt(field, value)
    }

    override fun putLong(field: String, value: Long) {
        editor.putLong(field, value)
    }

    override fun remove(field: String) {
        editor.remove(field)
    }

    override fun apply() {
        editor.apply()
    }
}
