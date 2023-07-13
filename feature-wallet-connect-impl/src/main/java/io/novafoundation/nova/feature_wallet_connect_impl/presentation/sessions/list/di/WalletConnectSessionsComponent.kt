package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload

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
            @BindsInstance payload: WalletConnectSessionsPayload
        ): WalletConnectSessionsComponent
    }

    fun inject(fragment: WalletConnectSessionsFragment)
}
