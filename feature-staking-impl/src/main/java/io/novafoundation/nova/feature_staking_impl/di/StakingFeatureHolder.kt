package io.novafoundation.nova.feature_staking_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_ahm_api.di.ChainMigrationFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.NominationPoolsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingDashboardRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.SelectMythosInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollatorSettings.SelectMythCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.common.SelectCollatorInterScreenCommunicator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.settings.SelectCollatorSettingsInterScreenCommunicator
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class StakingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: StakingRouter,
    private val parachainStakingRouter: ParachainStakingRouter,
    private val nominationPoolsRouter: NominationPoolsRouter,
    private val startMultiStakingRouter: StartMultiStakingRouter,
    private val stakingDashboardRouter: StakingDashboardRouter,
    private val mythosStakingRouter: MythosStakingRouter,
    private val selectAddressCommunicator: SelectAddressCommunicator,
    private val selectCollatorInterScreenCommunicator: SelectCollatorInterScreenCommunicator,
    private val selectCollatorSettingsInterScreenCommunicator: SelectCollatorSettingsInterScreenCommunicator,
    private val selectMythosCollatorInterScreenCommunicator: SelectMythosInterScreenCommunicator,
    private val selectMythosCollatorSettingsInterScreenCommunicator: SelectMythCollatorSettingsInterScreenCommunicator,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerStakingFeatureComponent_StakingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .dbApi(getFeature(DbApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .proxyFeatureApi(getFeature(ProxyFeatureApi::class.java))
            .dAppFeatureApi(getFeature(DAppFeatureApi::class.java))
            .chainMigrationFeatureApi(getFeature(ChainMigrationFeatureApi::class.java))
            .build()

        return DaggerStakingFeatureComponent.factory()
            .create(
                router = router,
                parachainStaking = parachainStakingRouter,
                mythosStakingRouter = mythosStakingRouter,
                selectCollatorInterScreenCommunicator = selectCollatorInterScreenCommunicator,
                selectCollatorSettingsInterScreenCommunicator = selectCollatorSettingsInterScreenCommunicator,
                selectAddressCommunicator = selectAddressCommunicator,
                nominationPoolsRouter = nominationPoolsRouter,
                startMultiStakingRouter = startMultiStakingRouter,
                stakingDashboardRouter = stakingDashboardRouter,
                selectMythosCollatorInterScreenCommunicator = selectMythosCollatorInterScreenCommunicator,
                selectMythosCollatorSettingsInterScreenCommunicator = selectMythosCollatorSettingsInterScreenCommunicator,
                deps = dependencies
            )
    }
}
