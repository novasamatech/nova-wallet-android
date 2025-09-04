package io.novafoundation.nova.feature_governance_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_deep_link_building.di.DeepLinkBuildingFeatureApi
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteCommunicator
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class GovernanceFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: GovernanceRouter,
    private val selectTracksCommunicator: SelectTracksCommunicator,
    private val tinderGovVoteCommunicator: TinderGovVoteCommunicator
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerGovernanceFeatureComponent_GovernanceFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .dAppFeatureApi(getFeature(DAppFeatureApi::class.java))
            .xcmFeatureApi(getFeature(XcmFeatureApi::class.java))
            .deepLinkBuildingFeatureApi(getFeature(DeepLinkBuildingFeatureApi::class.java))
            .build()

        return DaggerGovernanceFeatureComponent.factory()
            .create(
                accountFeatureDependencies,
                router,
                selectTracksCommunicator = selectTracksCommunicator,
                tinderGovVoteCommunicator = tinderGovVoteCommunicator
            )
    }
}
