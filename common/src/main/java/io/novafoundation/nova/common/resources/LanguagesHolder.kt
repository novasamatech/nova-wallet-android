package io.novafoundation.nova.common.resources

import io.novafoundation.nova.core.model.Language
import javax.inject.Singleton

@Singleton
class LanguagesHolder {

    companion object {

        private val ENGLISH = Language("en", "ENGLISH")
        private val CHINESE = Language("zh", "CHINESE")
        private val ITALIAN = Language("it", "ITALIAN")
        private val PORTUGUESE = Language("pt", "PORTUGUESE")
        private val RUSSIAN = Language("ru", "RUSSIAN")
        private val SPANISH = Language("es", "SPANISH")
        private val TURKISH = Language("tr", "TURKISH")
        private val INDONESIAN = Language("in", "INDONESIAN")

    }

    fun getDefaultLanguage(): Language {
        return ENGLISH
    }

    fun getLanguages(): List<Language> {
        val defaultLanguage = listOf(getDefaultLanguage())
        val otherLanguages = listOf(CHINESE, ITALIAN, PORTUGUESE, RUSSIAN, SPANISH, TURKISH, INDONESIAN)
        return defaultLanguage + otherLanguages.sortedBy { it.name }
    }
}
