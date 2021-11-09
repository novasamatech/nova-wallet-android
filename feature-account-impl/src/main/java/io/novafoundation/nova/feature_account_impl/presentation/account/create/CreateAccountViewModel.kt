package io.novafoundation.nova.feature_account_impl.presentation.account.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.data.mappers.mapNameChooserStateToOptionalName
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.ForcedChainMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithAccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithForcedChainMixin

class CreateAccountViewModel(
    private val router: AccountRouter,
    private val payload: AddAccountPayload,
    forcedChainMixinFactory: MixinFactory<ForcedChainMixin>,
    accountNameChooserFactory: MixinFactory<AccountNameChooserMixin.Presentation>,
) : BaseViewModel(),
    WithForcedChainMixin,
    WithAccountNameChooserMixin {

    override val forcedChainMixin: ForcedChainMixin = forcedChainMixinFactory.create(scope = this)
    override val accountNameChooser: AccountNameChooserMixin.Presentation = accountNameChooserFactory.create(scope = this)

    val nextButtonEnabledLiveData = accountNameChooser.nameValid

    private val _showScreenshotsWarningEvent = MutableLiveData<Event<Unit>>()
    val showScreenshotsWarningEvent: LiveData<Event<Unit>> = _showScreenshotsWarningEvent

    fun homeButtonClicked() {
        router.back()
    }

    fun nextClicked() {
        _showScreenshotsWarningEvent.value = Event(Unit)
    }

    fun screenshotWarningConfirmed() {
        val nameState = accountNameChooser.nameState.value!!

        router.openMnemonicScreen(mapNameChooserStateToOptionalName(nameState), payload)
    }
}
