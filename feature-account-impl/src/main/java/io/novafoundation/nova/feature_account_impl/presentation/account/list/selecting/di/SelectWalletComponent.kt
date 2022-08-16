package io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectWalletRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.list.selecting.SelectWalletFragment

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
            @BindsInstance request: SelectWalletRequester.Request
        ): SelectWalletComponent
    }

    fun inject(fragment: SelectWalletFragment)
}
