package io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.startCreateWallet.StartCreateWalletFragment

@Subcomponent(
    modules = [
        StartCreateWalletModule::class
    ]
)
@ScreenScope
interface StartCreateWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): StartCreateWalletComponent
    }

    fun inject(fragment: StartCreateWalletFragment)
}
