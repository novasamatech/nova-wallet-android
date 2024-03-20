package io.novafoundation.nova.feature_deep_linking.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkingRouter
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

class DeepLinkingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val deepLinkingRouter: DeepLinkingRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerDeepLinkingFeatureComponent_DeepLinkingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .governanceFeatureApi(getFeature(GovernanceFeatureApi::class.java))
            .dAppFeatureApi(getFeature(DAppFeatureApi::class.java))
            .build()

        return DaggerDeepLinkingFeatureComponent.factory()
            .create(
                deepLinkingRouter = deepLinkingRouter,
                deps = dependencies
            )
    }
}
