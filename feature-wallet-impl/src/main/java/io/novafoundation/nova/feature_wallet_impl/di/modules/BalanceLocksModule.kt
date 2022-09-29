package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.updaters.BalanceLocksUpdaterFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.updaters.locks.BalanceLocksUpdaterFactoryImpl

@Module
class BalanceLocksModule {

    @Provides
    @FeatureScope
    fun provideBalanceLocksUpdaterFactory(
        scope: AccountUpdateScope,
        assetSourceRegistry: AssetSourceRegistry,
    ): BalanceLocksUpdaterFactory {
        return BalanceLocksUpdaterFactoryImpl(
            scope,
            assetSourceRegistry
        )
    }
}
