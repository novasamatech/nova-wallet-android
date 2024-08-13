package io.novafoundation.nova.feature_account_impl.data.fee.types

import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.model.toFeePaymentAsset
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_impl.data.fee.utils.HydraDxQuoteSharedComputation
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetIdConverter
import io.novafoundation.nova.feature_swap_core.data.network.setFeeCurrency
import io.novafoundation.nova.feature_swap_core.data.network.toOnChainIdOrThrow
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

internal class HydrationConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val hydraDxAssetIdConverter: HydraDxAssetIdConverter,
    private val hydraDxQuoteSharedComputation: HydraDxQuoteSharedComputation,
    private val accountRepository: AccountRepository,
    private val coroutineScope: CoroutineScope
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        val baseCall = extrinsicBuilder.getCall()
        extrinsicBuilder.resetCalls()

        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.toOnChainIdOrThrow(paymentAsset))
        extrinsicBuilder.call(baseCall)
        extrinsicBuilder.setFeeCurrency(hydraDxAssetIdConverter.systemAssetId)
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val accountId = metaAccount.requireAccountIdIn(chain)
        val fromAsset = chain.commissionAsset

        val args = AssetExchangeQuoteArgs(
            chainAssetIn = fromAsset,
            chainAssetOut = paymentAsset,
            amount = BigInteger.ZERO,
            swapDirection = SwapDirection.SPECIFIED_IN
        )

        val assetConversion = hydraDxQuoteSharedComputation.getAssetConversion(chain, accountId, coroutineScope)
        val paths = hydraDxQuoteSharedComputation.paths(chain, args, accountId, coroutineScope)
        val quote = assetConversion.quote(paths, args)
        return SubstrateFee(quote.quote, nativeFee.submissionOrigin, paymentAsset.fullId)
    }

    override suspend fun availableCustomFeeAssets(): List<Chain.Asset> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val accountId = metaAccount.accountIdIn(chain)
        val fromAsset = chain.commissionAsset

        val allSwapDirections = hydraDxQuoteSharedComputation.directions(chain, accountId!!, coroutineScope)
        val commissionAssetDirections = allSwapDirections.adjacencyList[fromAsset.fullId] ?: emptyList()
        return commissionAssetDirections.map { chainRegistry.asset(it.direction.to) }
    }
}
