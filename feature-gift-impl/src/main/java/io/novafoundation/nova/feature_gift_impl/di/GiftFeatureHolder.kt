package io.novafoundation.nova.feature_gift_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class GiftFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: GiftRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerGiftFeatureComponent_GiftFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerGiftFeatureComponent.factory()
            .create(router, dependencies)
    }
}
