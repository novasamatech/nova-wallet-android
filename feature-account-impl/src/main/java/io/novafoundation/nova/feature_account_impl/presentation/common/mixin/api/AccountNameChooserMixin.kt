package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api

import androidx.lifecycle.LiveData

interface AccountNameChooserMixin {

    val nameState: LiveData<State>

    fun nameChanged(newName: String)

    interface Presentation : AccountNameChooserMixin {

        val nameValid: LiveData<Boolean>
    }

    sealed class State {

        object NoInput : State()

        class Input(val value: String) : State()
    }
}

interface WithAccountNameChooserMixin {

    val accountNameChooser: AccountNameChooserMixin
}
