package io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsRequester
import io.novafoundation.nova.feature_account_impl.presentation.account.list.multipleSelecting.SelectMultipleWalletsFragment

@Subcomponent(
    modules = [
        SelectMultipleWalletsModule::class
    ]
)
@ScreenScope
interface SelectMultipleWalletsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: SelectMultipleWalletsRequester.Request
        ): SelectMultipleWalletsComponent
    }

    fun inject(fragment: SelectMultipleWalletsFragment)
}
