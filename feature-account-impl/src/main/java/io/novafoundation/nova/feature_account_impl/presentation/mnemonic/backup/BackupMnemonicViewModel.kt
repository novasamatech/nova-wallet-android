package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionRequester
import io.novafoundation.nova.feature_account_impl.presentation.lastResponseOrDefault
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload.CreateExtras
import io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic.MnemonicWordModel
import io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic.mapMnemonicToMnemonicWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val accountName: String?,
    private val addAccountPayload: AddAccountPayload,
    private val advancedEncryptionInteractor: AdvancedEncryptionInteractor,
    private val advancedEncryptionRequester: AdvancedEncryptionRequester
) : BaseViewModel() {

    val mnemonicFlow = flowOf { generateMnemonic() }

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        advancedEncryptionRequester.openRequest(addAccountPayload)
    }


    fun nextClicked() = launch {
        val advancedEncryptionResponse = advancedEncryptionRequester.lastResponseOrDefault(addAccountPayload, advancedEncryptionInteractor)
        val mnemonic = mnemonicFlow.first().map(MnemonicWordModel::word)

        val payload = ConfirmMnemonicPayload(
            mnemonic = mnemonic,
            CreateExtras(
                accountName = accountName,
                addAccountPayload = addAccountPayload,
                advancedEncryptionPayload = advancedEncryptionResponse
            )
        )

        router.openConfirmMnemonicOnCreate(payload)
    }

    private suspend fun generateMnemonic(): List<MnemonicWordModel> {
        val mnemonic = interactor.generateMnemonic()

        return withContext(Dispatchers.Default) {
            mapMnemonicToMnemonicWords(mnemonic)
        }
    }
}
