package io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.SelectSingleWalletRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.list.singleSelecting.SelectSingleWalletFragment

@Subcomponent(
    modules = [
        SelectSingleWalletModule::class
    ]
)
@ScreenScope
interface SelectSingleWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: SelectSingleWalletRequester.Request
        ): SelectSingleWalletComponent
    }

    fun inject(fragment: SelectSingleWalletFragment)
}
