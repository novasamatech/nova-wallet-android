package io.novafoundation.nova.feature_account_impl.presentation.language.mapper

import io.novafoundation.nova.common.utils.capitalize
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageModel
import java.util.Locale

fun mapLanguageToLanguageModel(language: Language): LanguageModel {
    val languageLocale = Locale(language.iso639Code)
    return LanguageModel(
        language.iso639Code,
        languageLocale.displayLanguage.capitalize(),
        languageLocale.getDisplayLanguage(languageLocale).capitalize()
    )
}
