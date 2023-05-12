package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.WalletConnectApproveSessionFragment

@Subcomponent(
    modules = [
        WalletConnectApproveSessionModule::class
    ]
)
@ScreenScope
interface WalletConnectApproveSessionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): WalletConnectApproveSessionComponent
    }

    fun inject(fragment: WalletConnectApproveSessionFragment)
}
