package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.impl

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import kotlinx.coroutines.CoroutineScope

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

    override val nameState = MutableLiveData(maybeInputOf(""))

    override val nameValid: LiveData<Boolean> = nameState.map {
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
