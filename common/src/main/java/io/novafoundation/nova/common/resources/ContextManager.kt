package io.novafoundation.nova.common.resources

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import io.novafoundation.nova.common.data.storage.PreferencesImpl
import io.novafoundation.nova.common.di.modules.SHARED_PREFERENCES_FILE
import io.novafoundation.nova.common.utils.SingletonHolder
import java.util.Locale
import javax.inject.Singleton

@Singleton
class ContextManager private constructor(
    private var context: Context,
    private val languagesHolder: LanguagesHolder
) {

    private val LANGUAGE_PART_INDEX = 0
    private val COUNTRY_PART_INDEX = 1

    private var activity: AppCompatActivity? = null

    companion object : SingletonHolder<ContextManager, Context, LanguagesHolder>(::ContextManager)

    fun getApplicationContext(): Context {
        return context
    }

    fun getActivity(): AppCompatActivity? {
        return activity
    }

    fun attachActivity(activity: AppCompatActivity) {
        this.activity = activity
    }

    fun detachActivity() {
        this.activity = null
    }

    fun setLocale(context: Context): Context {
        return updateResources(context)
    }

    fun getLocale(): Locale {
        return if (Locale.getDefault().displayLanguage != "ba") Locale.getDefault() else Locale("ru")
    }

    private fun updateResources(context: Context): Context {
        val prefs = PreferencesImpl(context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE))

        val currentLanguage = prefs.getCurrentLanguage()
        val currentLanguageCode = if (currentLanguage == null) {
            val currentLocale = Locale.getDefault()
            if (languagesHolder.getLanguages().map { it.iso639Code }.contains(currentLocale.language)) {
                currentLocale.language
            } else {
                languagesHolder.getDefaultLanguage().iso639Code
            }
        } else {
            currentLanguage.iso639Code
        }

        prefs.saveCurrentLanguage(currentLanguageCode)

        val locale = mapLanguageToLocale(currentLanguageCode)
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        this.context = context.createConfigurationContext(configuration)

        return this.context
    }

    private fun mapLanguageToLocale(language: String): Locale {
        val codes = language.split("_")

        return if (hasCountryCode(codes)) {
            Locale(codes[LANGUAGE_PART_INDEX], codes[COUNTRY_PART_INDEX])
        } else {
            Locale(language)
        }
    }

    private fun hasCountryCode(codes: List<String>) = codes.size != 1
}

fun ContextManager.requireActivity(): Activity {
    return requireNotNull(getActivity())
}
