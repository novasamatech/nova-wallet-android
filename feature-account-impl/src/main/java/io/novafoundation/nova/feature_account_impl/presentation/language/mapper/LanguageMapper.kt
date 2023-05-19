package io.novafoundation.nova.feature_account_impl.presentation.language.mapper

import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageModel
import java.util.Locale

fun mapLanguageToLanguageModel(language: Language): LanguageModel {
    val languageLocale = Locale(language.iso)
    return LanguageModel(
        language.iso,
        languageLocale.displayLanguage.capitalize(),
        languageLocale.getDisplayLanguage(languageLocale).capitalize()
    )
}
