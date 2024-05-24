package io.novafoundation.nova.common.resources

import io.novafoundation.nova.core.model.Language
import javax.inject.Singleton

@Singleton
class LanguagesHolder {

    companion object {

        private val ENGLISH = Language("en")
        private val CHINESE = Language("zh")
        private val ITALIAN = Language("it")
        private val PORTUGUESE = Language("pt")
        private val RUSSIAN = Language("ru")
        private val SPANISH = Language("es")
        private val TURKISH = Language("tr")

        private val availableLanguages = mutableListOf(ENGLISH, CHINESE, ITALIAN, PORTUGUESE, RUSSIAN, SPANISH, TURKISH)
    }

    fun getDefaultLanguage(): Language {
        return ENGLISH
    }

    fun getLanguages(): List<Language> {
        return availableLanguages
    }
}
