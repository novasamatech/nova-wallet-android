package io.novafoundation.nova.feature_dapp_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.di.AddToFavouritesComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.di.AuthorizedDAppsComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.extrinsicDetails.di.DAppExtrinsicDetailsComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di.DAppBrowserComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.di.DAppSignComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.main.di.MainDAppComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.di.DAppSearchComponent
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.scan.di.WalletConnectScanComponent
import io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions.di.WalletConnectSessionsComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.web3names.di.Web3NamesApi

@Component(
    dependencies = [
        DAppFeatureDependencies::class
    ],
    modules = [
        DappFeatureModule::class
    ]
)
@FeatureScope
interface DAppFeatureComponent : DAppFeatureApi {

    // Screens

    fun mainComponentFactory(): MainDAppComponent.Factory

    fun browserComponentFactory(): DAppBrowserComponent.Factory

    fun signExtrinsicComponentFactory(): DAppSignComponent.Factory

    fun extrinsicDetailsComponentFactory(): DAppExtrinsicDetailsComponent.Factory

    fun dAppSearchComponentFactory(): DAppSearchComponent.Factory

    fun addToFavouritesComponentFactory(): AddToFavouritesComponent.Factory

    fun authorizedDAppsComponentFactory(): AuthorizedDAppsComponent.Factory

    fun walletConnectSessionsComponentFactory(): WalletConnectSessionsComponent.Factory

    fun walletConnectScanComponentFactory(): WalletConnectScanComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: DAppRouter,
            @BindsInstance signCommunicator: DAppSignCommunicator,
            @BindsInstance searchCommunicator: DAppSearchCommunicator,
            @BindsInstance walletConnectScanCommunicator: WalletConnectScanCommunicator,
            deps: DAppFeatureDependencies
        ): DAppFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            RuntimeApi::class,
            CurrencyFeatureApi::class,
            Web3NamesApi::class
        ]
    )
    interface DAppFeatureDependenciesComponent : DAppFeatureDependencies
}
