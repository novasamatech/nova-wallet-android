package io.novafoundation.nova.feature_account_impl.presentation.language

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageModel
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_account_impl.presentation.language.mapper.mapLanguageToLanguageModel

internal class RealLanguageUseCase(
    private val accountInteractor: AccountInteractor,
) : LanguageUseCase {

    override suspend fun selectedLanguageModel(): LanguageModel {
        return mapLanguageToLanguageModel(accountInteractor.getSelectedLanguage())
    }
}
