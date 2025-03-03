package io.novafoundation.nova.feature_push_notifications.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectMultipleWalletsCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectTracksCommunicator
import io.novafoundation.nova.feature_deep_link_building.di.DeepLinkBuildingFeatureApi
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.data.PushNotificationsService
import io.novafoundation.nova.feature_push_notifications.presentation.governance.PushGovernanceSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.governance.di.PushGovernanceSettingsComponent
import io.novafoundation.nova.feature_push_notifications.presentation.settings.di.PushSettingsComponent
import io.novafoundation.nova.feature_push_notifications.presentation.staking.PushStakingSettingsCommunicator
import io.novafoundation.nova.feature_push_notifications.presentation.staking.di.PushStakingSettingsComponent
import io.novafoundation.nova.feature_push_notifications.presentation.welcome.di.PushWelcomeComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        PushNotificationsFeatureDependencies::class
    ],
    modules = [
        PushNotificationsFeatureModule::class
    ]
)
@FeatureScope
interface PushNotificationsFeatureComponent : PushNotificationsFeatureApi {

    fun getPushNotificationService(): PushNotificationsService

    fun pushWelcomeComponentFactory(): PushWelcomeComponent.Factory

    fun pushSettingsComponentFactory(): PushSettingsComponent.Factory

    fun pushGovernanceSettings(): PushGovernanceSettingsComponent.Factory

    fun pushStakingSettings(): PushStakingSettingsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: PushNotificationsRouter,
            @BindsInstance selectMultipleWalletsCommunicator: SelectMultipleWalletsCommunicator,
            @BindsInstance selectTracksCommunicator: SelectTracksCommunicator,
            @BindsInstance pushGovernanceSettingsCommunicator: PushGovernanceSettingsCommunicator,
            @BindsInstance pushStakingSettingsCommunicator: PushStakingSettingsCommunicator,
            deps: PushNotificationsFeatureDependencies
        ): PushNotificationsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            GovernanceFeatureApi::class,
            DeepLinkBuildingFeatureApi::class,
            WalletFeatureApi::class
        ]
    )
    interface PushNotificationsFeatureDependenciesComponent : PushNotificationsFeatureDependencies
}
