package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.assetConversion
import io.novafoundation.nova.common.utils.mutableMultiMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.CompoundMultiLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.ForeignAssetsLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.LocalAssetsLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.MultiLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.NativeAssetLocationConverter
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter.toMultiLocationOrThrow
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.api.assetConversionOrNull
import io.novafoundation.nova.feature_swap_impl.data.network.blockhain.api.pools
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.toEncodableInstance
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions.assetTxPayment
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.BooleanType
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.call

class AssetConversionExchangeFactory(
    private val chainRegistry: ChainRegistry,
    private val remoteStorageSource: StorageDataSource,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val extrinsicService: ExtrinsicService,
) : AssetExchange.Factory {

    override suspend fun create(chainId: ChainId): AssetExchange? {
        val chain = chainRegistry.getChainOrNull(chainId) ?: return null
        val runtime = chainRegistry.getRuntime(chainId)

        val converter = CompoundMultiLocationConverter(
            NativeAssetLocationConverter(chain),
            LocalAssetsLocationConverter(chain, runtime),
            ForeignAssetsLocationConverter(chain, runtime)
        )

        return AssetConversionExchange(
            chain = chain,
            multiLocationConverter = converter,
            remoteStorageSource = remoteStorageSource,
            multiChainRuntimeCallsApi = runtimeCallsApi,
            extrinsicService = extrinsicService
        )
    }
}

private class AssetConversionExchange(
    private val chain: Chain,
    private val multiLocationConverter: MultiLocationConverter,
    private val remoteStorageSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val extrinsicService: ExtrinsicService,
) : AssetExchange {

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAllAvailableDirections(allPools)
        }
    }

    override suspend fun quote(args: SwapQuoteArgs): AssetExchangeQuote {
        val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)
        val quotedBalance = runtimeCallsApi.quote(
            swapDirection = args.swapDirection,
            assetIn = args.tokenIn.configuration,
            assetOut = args.tokenOut.configuration,
            amount = args.amount
        ) ?: throw SwapQuoteException.NotEnoughLiquidity

        return AssetExchangeQuote(quote = quotedBalance)
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        val nativeAssetFee = extrinsicService.estimateFeeV2(chain) {
            executeSwap(args, origin = chain.emptyAccountId())
        }

        val converted = convertNativeFeeToPayingTokenFee(nativeAssetFee, args)

        return AssetExchangeFee(networkFee = converted)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicHash> {
        return extrinsicService.submitExtrinsicWithSelectedWallet(chain) { origin ->
            executeSwap(args, origin)
        }
    }

    private suspend fun constructAllAvailableDirections(pools: List<Pair<MultiLocation, MultiLocation>>): MultiMap<FullChainAssetId, FullChainAssetId> {
        val multiMap = mutableMultiMapOf<FullChainAssetId, FullChainAssetId>()

        pools.forEach { (firstLocation, secondLocation) ->
            val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@forEach
            val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@forEach

            val firstAssetId = firstAsset.fullId
            val secondAssetId = secondAsset.fullId

            multiMap.put(firstAssetId, secondAssetId)
            multiMap.put(secondAssetId, firstAssetId)
        }

        return multiMap
    }

    private suspend fun convertNativeFeeToPayingTokenFee(nativeTokenFee: Fee, args: SwapExecuteArgs): Fee {
        val customFeeAsset = args.customFeeAsset

        return  if (customFeeAsset != null) {
            val converted = multiChainRuntimeCallsApi.forChain(chain.id).quote(
                swapDirection = SwapDirection.SPECIFIED_OUT,
                assetIn = customFeeAsset,
                assetOut = chain.utilityAsset,
                amount = nativeTokenFee.amount
            )

            InlineFee(requireNotNull(converted))
        } else {
            nativeTokenFee
        }
    }

    private suspend fun ExtrinsicBuilder.executeSwap(swapExecuteArgs: SwapExecuteArgs, origin: AccountId) {
        val path = listOf(swapExecuteArgs.assetIn, swapExecuteArgs.assetOut)
            .map { asset -> multiLocationConverter.encodableMultiLocationOf(asset) }

        val keepAlive = false

        when (val swapLimit = swapExecuteArgs.swapLimit) {
            is SwapLimit.SpecifiedIn -> call(
                moduleName = Modules.ASSET_CONVERSION,
                callName = "swap_exact_tokens_for_tokens",
                arguments = mapOf(
                    "path" to path,
                    "amount_in" to swapLimit.amountIn,
                    "amount_out_min" to swapLimit.amountOutMin,
                    "send_to" to origin,
                    "keep_alive" to keepAlive
                )
            )

            is SwapLimit.SpecifiedOut -> call(
                moduleName = Modules.ASSET_CONVERSION,
                callName = "swap_tokens_for_exact_tokens",
                arguments = mapOf(
                    "path" to path,
                    "amount_out" to swapLimit.amountOut,
                    "amount_in_max" to swapLimit.amountInMax,
                    "send_to" to origin,
                    "keep_alive" to keepAlive
                )
            )
        }

        setFeeAsset(swapExecuteArgs.customFeeAsset)
    }

    private suspend fun ExtrinsicBuilder.setFeeAsset(feeAsset: Chain.Asset?) {
        if (feeAsset == null) return

        val assetId = multiLocationConverter.encodableMultiLocationOf(feeAsset)

        assetTxPayment(assetId)
    }

    private suspend fun MultiLocationConverter.encodableMultiLocationOf(chainAsset: Chain.Asset): Any? {
        return toMultiLocationOrThrow(chainAsset).toEncodableInstance()
    }

    private suspend fun RuntimeCallsApi.quote(
        swapDirection: SwapDirection,
        assetIn: Chain.Asset,
        assetOut: Chain.Asset,
        amount: Balance,
    ): Balance? {
        val method = when (swapDirection) {
            SwapDirection.SPECIFIED_IN -> "quote_price_exact_tokens_for_tokens"
            SwapDirection.SPECIFIED_OUT -> "quote_price_tokens_for_exact_tokens"
        }

        val asset1 = multiLocationConverter.toMultiLocationOrThrow(assetIn).toEncodableInstance()
        val asset2 = multiLocationConverter.toMultiLocationOrThrow(assetOut).toEncodableInstance()

        val includeFee = true

        val multiLocationTypeName = runtime.metadata.assetIdTypeName()

        return call(
            section = "AssetConversionApi",
            method = method,
            arguments = listOf(
                asset1 to multiLocationTypeName,
                asset2 to multiLocationTypeName,
                amount to "Balance",
                includeFee to BooleanType.name
            ),
            returnType = "Option<Balance>",
            returnBinding = ::bindNumberOrNull
        )
    }

    private fun RuntimeMetadata.assetIdTypeName(): String {
        val (assetIdArgument) = assetConversion().call("add_liquidity").arguments

        val assetIdType = assetIdArgument.type!!

        return assetIdType.name
    }
}