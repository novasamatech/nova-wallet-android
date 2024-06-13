package io.novafoundation.nova.feature_onboarding_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import javax.inject.Inject

@ApplicationScope
class OnboardingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val onboardingRouter: OnboardingRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val onboardingFeatureDependencies = DaggerOnboardingFeatureComponent_OnboardingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .ledgerCoreApi(getFeature(LedgerCoreApi::class.java))
            .build()
        return DaggerOnboardingFeatureComponent.factory()
            .create(onboardingRouter, onboardingFeatureDependencies)
    }
}
