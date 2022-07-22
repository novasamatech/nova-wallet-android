package io.novafoundation.nova.feature_account_impl.presentation.account.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountListFragment

@Subcomponent(
    modules = [
        AccountListModule::class
    ]
)
@ScreenScope
interface AccountListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AccountListComponent
    }

    fun inject(fragment: AccountListFragment)
}
