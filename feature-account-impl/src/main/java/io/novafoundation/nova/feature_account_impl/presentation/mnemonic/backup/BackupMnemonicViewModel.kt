package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup

import androidx.lifecycle.liveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
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
    private val advancedEncryptionCommunicator: AdvancedEncryptionCommunicator,
    cryptoTypeChooserMixinFactory: MixinFactory<CryptoTypeChooserMixin>,
) : BaseViewModel(),
    WithCryptoTypeChooserMixin {

    val mnemonicLiveData = liveData {
        emit(generateMnemonic())
    }

    override val cryptoTypeChooserMixin = cryptoTypeChooserMixinFactory.create(scope = this)

    fun homeButtonClicked() {
        router.back()
    }

    fun optionsClicked() {
        val request = AdvancedEncryptionCommunicator.Request()

        advancedEncryptionCommunicator.openRequest(request)
    }


    fun nextClicked(derivationPath: String) = launch {
        val cryptoTypeModel = cryptoTypeChooserMixin.selectedEncryptionTypeFlow.first()

        val mnemonicWords = mnemonicLiveData.value ?: return@launch

        val mnemonic = mnemonicWords.map(MnemonicWordModel::word)

        val payload = ConfirmMnemonicPayload(
            mnemonic,
            CreateExtras(
                accountName,
                cryptoTypeModel.cryptoType,
                addAccountPayload,
                derivationPath
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
