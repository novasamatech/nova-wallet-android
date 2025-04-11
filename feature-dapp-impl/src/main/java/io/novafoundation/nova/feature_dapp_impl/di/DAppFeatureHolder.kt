package io.novafoundation.nova.feature_dapp_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_banners_api.di.BannersFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchCommunicator
import io.novafoundation.nova.feature_deep_linking.di.DeepLinkingFeatureApi
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class DAppFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: DAppRouter,
    private val signCommunicator: ExternalSignCommunicator,
    private val searchCommunicator: DAppSearchCommunicator,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dApp = DaggerDAppFeatureComponent_DAppFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .bannersFeatureApi(getFeature(BannersFeatureApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .deepLinkingFeatureApi(getFeature(DeepLinkingFeatureApi::class.java))
            .build()

        return DaggerDAppFeatureComponent.factory()
            .create(router, signCommunicator, searchCommunicator, dApp)
    }
}
