package io.novafoundation.nova.feature_account_impl.presentation.account.list.switching.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.list.switching.SwitchWalletFragment

@Subcomponent(
    modules = [
        SwitchWalletModule::class
    ]
)
@ScreenScope
interface SwitchWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SwitchWalletComponent
    }

    fun inject(fragment: SwitchWalletFragment)
}
