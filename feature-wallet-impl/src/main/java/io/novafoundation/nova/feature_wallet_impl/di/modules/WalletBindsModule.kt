package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.dryRun.AssetIssuerRegistry
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing.RealAssetIssuerRegistry
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealStatemineAssetsRepository

@Module
interface WalletBindsModule {

    @Binds
    fun bindStatemineAssetRepository(implementation: RealStatemineAssetsRepository): StatemineAssetsRepository

    @Binds
    fun bindAssetIssuerRegistry(implementation: RealAssetIssuerRegistry): AssetIssuerRegistry
}
