package io.novafoundation.nova.feature_settings_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_push_notifications.data.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi

import javax.inject.Inject

@ApplicationScope
class SettingsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: SettingsRouter,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerSettingsFeatureComponent_SettingsFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .pushNotificationsFeatureApi(getFeature(PushNotificationsFeatureApi::class.java))
            .build()

        return DaggerSettingsFeatureComponent.factory()
            .create(
                router = router,
                deps = accountFeatureDependencies
            )
    }
}
