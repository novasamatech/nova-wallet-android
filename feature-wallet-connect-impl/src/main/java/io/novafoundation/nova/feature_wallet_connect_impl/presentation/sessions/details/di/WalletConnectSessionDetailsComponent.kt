package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsFragment
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload

@Subcomponent(
    modules = [
        WalletConnectSessionDetailsModule::class
    ]
)
@ScreenScope
interface WalletConnectSessionDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: WalletConnectSessionDetailsPayload,
        ): WalletConnectSessionDetailsComponent
    }

    fun inject(fragment: WalletConnectSessionDetailsFragment)
}
