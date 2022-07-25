package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change.ChangeWatchAccountFragment

@Subcomponent(
    modules = [
        ChangeWatchAccountModule::class
    ]
)
@ScreenScope
interface ChangeWatchAccountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddAccountPayload.ChainAccount
        ): ChangeWatchAccountComponent
    }

    fun inject(fragment: ChangeWatchAccountFragment)
}
