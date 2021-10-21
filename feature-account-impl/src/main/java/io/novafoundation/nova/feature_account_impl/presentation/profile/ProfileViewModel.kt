package io.novafoundation.nova.feature_account_impl.presentation.profile

import androidx.lifecycle.liveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.language.mapper.mapLanguageToLanguageModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val addressIconGenerator: AddressIconGenerator,
) : BaseViewModel() {

    val selectedAccountFlow = interactor.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val accountIconFlow = selectedAccountFlow.map {
        addressIconGenerator.createAddressIcon(it.substrateAccountId, AddressIconGenerator.SIZE_BIG)
    }
        .inBackground()
        .share()

    val selectedLanguageLiveData = liveData {
        val language = interactor.getSelectedLanguage()

        emit(mapLanguageToLanguageModel(language))
    }

    fun aboutClicked() {
        router.openAboutScreen()
    }

    fun accountsClicked() {
        router.openAccounts(AccountChosenNavDirection.MAIN)
    }

    fun networksClicked() {
        router.openNodes()
    }

    fun languagesClicked() {
        router.openLanguages()
    }

    fun changePinCodeClicked() {
        router.openChangePinCode()
    }

    fun accountActionsClicked() = launch {
        router.openAccountDetails(selectedAccountFlow.first().id)
    }
}
