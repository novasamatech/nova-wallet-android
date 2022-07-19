package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create.CreateWatchWalletFragment

@Subcomponent(
    modules = [
        CreateWatchWalletModule::class
    ]
)
@ScreenScope
interface CreateWatchWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): CreateWatchWalletComponent
    }

    fun inject(fragment: CreateWatchWalletFragment)
}
