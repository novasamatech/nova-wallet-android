package io.novafoundation.nova.feature_wallet_connect_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.caip.di.CaipApi
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di.WalletConnectSessionsComponent
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan.di.WalletConnectScanComponent
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        WalletConnectFeatureDependencies::class
    ],
    modules = [
        WalletConnectFeatureModule::class
    ]
)
@FeatureScope
interface WalletConnectFeatureComponent : WalletConnectFeatureApi {

    fun walletConnectSessionsComponentFactory(): WalletConnectSessionsComponent.Factory

    fun walletConnectScanComponentFactory(): WalletConnectScanComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: WalletConnectRouter,
            @BindsInstance signCommunicator: ExternalSignCommunicator,
            @BindsInstance walletConnectScanCommunicator: WalletConnectScanCommunicator,
            deps: WalletConnectFeatureDependencies
        ): WalletConnectFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            AccountFeatureApi::class,
            RuntimeApi::class,
            CaipApi::class
        ]
    )
    interface WalletConnectFeatureDependenciesComponent : WalletConnectFeatureDependencies
}
