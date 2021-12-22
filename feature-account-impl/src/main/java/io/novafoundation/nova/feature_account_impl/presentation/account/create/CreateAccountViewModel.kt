package io.novafoundation.nova.feature_account_impl.presentation.account.create

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.data.mappers.mapNameChooserStateToOptionalName
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.WithAccountNameChooserMixin

class CreateAccountViewModel(
    private val router: AccountRouter,
    private val payload: AddAccountPayload,
    accountNameChooserFactory: MixinFactory<AccountNameChooserMixin.Presentation>,
) : BaseViewModel(),
    WithAccountNameChooserMixin {

    override val accountNameChooser: AccountNameChooserMixin.Presentation = accountNameChooserFactory.create(scope = this)

    val nextButtonEnabledLiveData = accountNameChooser.nameValid

    fun homeButtonClicked() {
        router.back()
    }

    fun nextClicked() {
        val nameState = accountNameChooser.nameState.value!!

        router.openMnemonicScreen(mapNameChooserStateToOptionalName(nameState), payload)
    }
}
