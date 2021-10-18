package jp.co.soramitsu.feature_account_impl.presentation.mnemonic.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.CryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithCryptoTypeChooserMixin
import jp.co.soramitsu.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload
import jp.co.soramitsu.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicPayload.CreateExtras
import jp.co.soramitsu.feature_account_impl.presentation.view.mnemonic.MnemonicWordModel
import jp.co.soramitsu.feature_account_impl.presentation.view.mnemonic.mapMnemonicToMnemonicWords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupMnemonicViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val accountName: String,
    private val addAccountPayload: AddAccountPayload,
    cryptoTypeChooserMixinFactory: MixinFactory<CryptoTypeChooserMixin>
) : BaseViewModel(),
    WithCryptoTypeChooserMixin {

    val mnemonicLiveData = liveData {
        emit(generateMnemonic())
    }

    private val _showInfoEvent = MutableLiveData<Event<Unit>>()
    val showInfoEvent: LiveData<Event<Unit>> = _showInfoEvent

    override val cryptoTypeChooserMixin = cryptoTypeChooserMixinFactory.create(scope = this)

    fun homeButtonClicked() {
        router.back()
    }

    fun infoClicked() {
        _showInfoEvent.value = Event(Unit)
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
