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
        private val FRENCH = Language("fr", "FRENCH")
        private val INDONESIAN = Language("in", "INDONESIAN")
        private val POLISH = Language("pl", "POLISH")
        private val JAPANESE = Language("ja", "JAPANESE")
        private val VIETNAMESE = Language("vi", "VIETNAMESE")
        private val KOREAN = Language("ko", "KOREAN")
        private val HUNGARIAN = Language("hu", "HUNGARIAN")
    }

    fun getDefaultLanguage(): Language {
        return ENGLISH
    }

    fun getLanguages(): List<Language> {
        val defaultLanguage = listOf(getDefaultLanguage())
        val otherLanguages = listOf(CHINESE, FRENCH, HUNGARIAN, INDONESIAN, ITALIAN, JAPANESE, KOREAN, POLISH, PORTUGUESE, RUSSIAN, SPANISH, TURKISH, VIETNAMESE)
        return defaultLanguage + otherLanguages.sortedBy { it.name }
    }
}
