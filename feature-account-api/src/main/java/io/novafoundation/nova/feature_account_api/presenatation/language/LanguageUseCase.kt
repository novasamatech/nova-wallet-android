package io.novafoundation.nova.feature_account_api.presenatation.language

interface LanguageUseCase {

    suspend fun selectedLanguageModel(): LanguageModel
}
