package io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.SelectWalletFragment

@Subcomponent(
    modules = [
        SelectWalletModule::class
    ]
)
@ScreenScope
interface SelectWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SelectWalletComponent
    }

    fun inject(fragment: SelectWalletFragment)
}
