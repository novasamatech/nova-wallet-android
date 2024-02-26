package io.novafoundation.nova.feature_push_notifications.data.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.data.presentation.staking.PushStakingSettingsCommunicator
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

class PushNotificationsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: PushNotificationsRouter,
    private val selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
    private val selectTracksCommunicator: SelectTracksCommunicator,
    private val pushGovernanceSettingsCommunicator: PushGovernanceSettingsCommunicator,
    private val pushStakingSettingsCommunicator: PushStakingSettingsCommunicator
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerPushNotificationsFeatureComponent_PushNotificationsFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .governanceFeatureApi(getFeature(GovernanceFeatureApi::class.java))
            .build()

        return DaggerPushNotificationsFeatureComponent.factory()
            .create(
                router,
                selectMultipleWalletsCommunicator,
                selectTracksCommunicator,
                pushGovernanceSettingsCommunicator,
                pushStakingSettingsCommunicator,
                dependencies
            )
    }
}
