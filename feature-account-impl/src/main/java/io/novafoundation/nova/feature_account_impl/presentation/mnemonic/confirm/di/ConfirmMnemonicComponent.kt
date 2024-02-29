package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.ConfirmMnemonicFragment
import io.novafoundation.nova.feature_account_api.presenatation.mnemonic.confirm.ConfirmMnemonicPayload

@Subcomponent(
    modules = [
        ConfirmMnemonicModule::class
    ]
)
@ScreenScope
interface ConfirmMnemonicComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ConfirmMnemonicPayload
        ): ConfirmMnemonicComponent
    }

    fun inject(confirmMnemonicFragment: ConfirmMnemonicFragment)
}
