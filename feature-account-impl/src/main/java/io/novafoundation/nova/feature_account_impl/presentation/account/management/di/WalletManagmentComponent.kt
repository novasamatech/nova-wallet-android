package io.novafoundation.nova.feature_account_impl.presentation.account.management.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.management.WalletManagmentFragment

@Subcomponent(
    modules = [
        WalletManagmentModule::class
    ]
)
@ScreenScope
interface WalletManagmentComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): WalletManagmentComponent
    }

    fun inject(fragment: WalletManagmentFragment)
}
