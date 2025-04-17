package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.BigRational
import io.novafoundation.nova.common.utils.fixedU128
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core_api.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydrationPriceConversionFallback
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Inject
import javax.inject.Named

@FeatureScope
internal class RealHydrationPriceConversionFallback @Inject constructor(
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    @Named(REMOTE_STORAGE_SOURCE)
    private val remoteStorageSource: StorageDataSource,
) : HydrationPriceConversionFallback {

    override suspend fun convertNativeAmount(amount: BalanceOf, conversionTarget: Chain.Asset): BalanceOf {
        if (conversionTarget.isUtilityAsset) return amount

        val targetOnChainId = hydraDxAssetIdConverter.toOnChainIdOrThrow(conversionTarget)

        val fallbackPrice = remoteStorageSource.query(conversionTarget.chainId) {
            metadata.multiTransactionPayment.acceptedCurrencies.query(targetOnChainId)
        } ?: error("No fallback price found")

        val fallbackPriceFractional = BigRational.fixedU128(fallbackPrice).quotient
        val converted = fallbackPriceFractional * amount.toBigDecimal()

        return converted.toBigInteger()
    }
}
