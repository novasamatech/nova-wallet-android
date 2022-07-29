package io.novafoundation.nova.feature_account_impl.presentation.account.select.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.select.AccountSwitchFragment

@Subcomponent(
    modules = [
        AccountSwitchModule::class
    ]
)
@ScreenScope
interface AccountSwitchComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AccountSwitchComponent
    }

    fun inject(fragment: AccountSwitchFragment)
}
