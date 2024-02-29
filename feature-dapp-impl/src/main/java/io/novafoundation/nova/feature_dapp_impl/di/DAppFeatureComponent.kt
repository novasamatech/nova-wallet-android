package io.novafoundation.nova.feature_dapp_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.addToFavourites.di.AddToFavouritesComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.di.AuthorizedDAppsComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.di.DAppBrowserComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.main.di.MainDAppComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.search.di.DAppSearchComponent
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

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

    fun dAppSearchComponentFactory(): DAppSearchComponent.Factory

    fun addToFavouritesComponentFactory(): AddToFavouritesComponent.Factory

    fun authorizedDAppsComponentFactory(): AuthorizedDAppsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: DAppRouter,
            @BindsInstance signCommunicator: ExternalSignCommunicator,
            @BindsInstance searchCommunicator: DAppSearchCommunicator,
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
        ]
    )
    interface DAppFeatureDependenciesComponent : DAppFeatureDependencies
}
