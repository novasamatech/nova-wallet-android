package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AccountNameChooserFactory(
    private val payload: AddAccountPayload,
) : MixinFactory<AccountNameChooserMixin.Presentation> {

    override fun create(scope: CoroutineScope): AccountNameChooserMixin.Presentation {
        return AccountNameChooserProvider(payload)
    }
}

class AccountNameChooserProvider(
    private val addAccountPayload: AddAccountPayload,
) : AccountNameChooserMixin.Presentation {

    override fun nameChanged(newName: String) {
        nameState.value = maybeInputOf(newName)
    }

    override val nameState = MutableStateFlow(maybeInputOf(""))

    override val nameValid: Flow<Boolean> = nameState.map {
        when (it) {
            is AccountNameChooserMixin.State.NoInput -> true
            is AccountNameChooserMixin.State.Input -> it.value.isNotEmpty()
        }
    }

    private fun maybeInputOf(value: String) = when (addAccountPayload) {
        is AddAccountPayload.MetaAccount -> AccountNameChooserMixin.State.Input(value)
        is AddAccountPayload.ChainAccount -> AccountNameChooserMixin.State.NoInput
    }
}
