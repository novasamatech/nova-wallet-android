package jp.co.soramitsu.feature_account_impl.presentation.account.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.mixin.MixinFactory
import jp.co.soramitsu.common.utils.Event
import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import jp.co.soramitsu.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin

class CreateAccountViewModel(
    private val router: AccountRouter,
    private val payload: AddAccountPayload,
    forcedChainMixinFactory: MixinFactory<ForcedChainMixin>
) : BaseViewModel(),
    WithForcedChainMixin {

    override val forcedChainMixin: ForcedChainMixin = forcedChainMixinFactory.create(scope = this)

    private val _nextButtonEnabledLiveData = MutableLiveData<Boolean>()
    val nextButtonEnabledLiveData: LiveData<Boolean> = _nextButtonEnabledLiveData

    private val _showScreenshotsWarningEvent = MutableLiveData<Event<Unit>>()
    val showScreenshotsWarningEvent: LiveData<Event<Unit>> = _showScreenshotsWarningEvent

    fun homeButtonClicked() {
        router.back()
    }

    fun accountNameChanged(accountName: CharSequence) {
        _nextButtonEnabledLiveData.value = accountName.isNotEmpty()
    }

    fun nextClicked() {
        _showScreenshotsWarningEvent.value = Event(Unit)
    }

    fun screenshotWarningConfirmed(accountName: String) {
        router.openMnemonicScreen(accountName, payload)
    }
}
