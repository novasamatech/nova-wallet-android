package io.novafoundation.nova.feature_account_impl.presentation.account.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.details.WalletDetailsFragment

@Subcomponent(
    modules = [
        AccountDetailsModule::class
    ]
)
@ScreenScope
interface AccountDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance metaId: Long
        ): AccountDetailsComponent
    }

    fun inject(fragment: WalletDetailsFragment)
}
