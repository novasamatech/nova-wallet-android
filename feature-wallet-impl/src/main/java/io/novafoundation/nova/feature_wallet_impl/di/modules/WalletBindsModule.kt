package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.StatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.MultisigExtrinsicValidationFactory
import io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.RealAccountInfoRepository
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing.AssetIssuerRegistry
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic.dryRun.issuing.RealAssetIssuerRegistry
import io.novafoundation.nova.feature_wallet_impl.data.repository.RealStatemineAssetsRepository
import io.novafoundation.nova.feature_wallet_impl.domain.validaiton.multisig.RealMultisigExtrinsicValidationFactory

@Module
internal interface WalletBindsModule {

    @Binds
    fun bindStatemineAssetRepository(implementation: RealStatemineAssetsRepository): StatemineAssetsRepository

    @Binds
    fun bindAssetIssuerRegistry(implementation: RealAssetIssuerRegistry): AssetIssuerRegistry

    @Binds
    fun bindMultisigExtrinsicValidationFactory(implementation: RealMultisigExtrinsicValidationFactory): MultisigExtrinsicValidationFactory

    @Binds
    fun bindAccountInfoRepository(implementation: RealAccountInfoRepository): AccountInfoRepository
}
