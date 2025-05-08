package io.novafoundation.nova.feature_vote.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_vote.presentation.VoteRouter
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import javax.inject.Inject

@ApplicationScope
class VoteFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: VoteRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerVoteFeatureComponent_VoteFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .build()
        return DaggerVoteFeatureComponent.factory()
            .create(router, dependencies)
    }
}
