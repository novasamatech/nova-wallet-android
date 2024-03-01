package io.novafoundation.nova.feature_deep_linking.di

import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.AccountRouter
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_api.presentation.DAppRouter
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

class DeepLinkingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val assetsRouter: AssetsRouter,
    private val governanceRouter: GovernanceRouter,
    private val accountRouter: AccountRouter,
    private val dAppRouter: DAppRouter,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerDeepLinkingFeatureComponent_DeepLinkingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .governanceFeatureApi(getFeature(GovernanceFeatureApi::class.java))
            .dAppFeatureApi(getFeature(DAppFeatureApi::class.java))
            .assetsFeatureApi(getFeature(AssetsFeatureApi::class.java))
            .build()

        return DaggerDeepLinkingFeatureComponent.factory()
            .create(
                assetsRouter = assetsRouter,
                governanceRouter = governanceRouter,
                accountRouter = accountRouter,
                dAppRouter = dAppRouter,
                deps = dependencies
            )
    }
}
