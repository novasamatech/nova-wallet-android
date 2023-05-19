package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan.WalletConnectScanFragment

@Subcomponent(
    modules = [
        WalletConnectScanModule::class
    ]
)
@ScreenScope
interface WalletConnectScanComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): WalletConnectScanComponent
    }

    fun inject(fragment: WalletConnectScanFragment)
}
