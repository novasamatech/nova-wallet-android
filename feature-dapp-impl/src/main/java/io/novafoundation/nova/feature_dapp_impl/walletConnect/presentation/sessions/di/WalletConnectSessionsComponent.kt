package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.WalletConnectSessionsFragment

@Subcomponent(
    modules = [
        WalletConnectSessionsModule::class
    ]
)
@ScreenScope
interface WalletConnectSessionsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): WalletConnectSessionsComponent
    }

    fun inject(fragment: WalletConnectSessionsFragment)
}
