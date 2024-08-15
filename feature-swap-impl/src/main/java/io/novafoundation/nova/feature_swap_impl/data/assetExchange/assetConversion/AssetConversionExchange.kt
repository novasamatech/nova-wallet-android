package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.common.utils.assetConversion
import io.novafoundation.nova.common.utils.mutableMultiMapOf
import io.novafoundation.nova.common.utils.put
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.MinimumBalanceBuyIn
import io.novafoundation.nova.feature_swap_core.domain.model.QuotePath
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchangeFee
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuote
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.AssetExchangeQuoteArgs
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions.assetTxPayment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.toMultiLocationOrThrow
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.call
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

class AssetConversionExchangeFactory(
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val remoteStorageSource: StorageDataSource,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val extrinsicService: ExtrinsicService,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
) : AssetExchange.Factory {

    override suspend fun create(chain: Chain, coroutineScope: CoroutineScope): AssetExchange {
        val converter = multiLocationConverterFactory.default(chain, coroutineScope)

        return AssetConversionExchange(
            chain = chain,
            multiLocationConverter = converter,
            remoteStorageSource = remoteStorageSource,
            multiChainRuntimeCallsApi = runtimeCallsApi,
            extrinsicService = extrinsicService,
            assetSourceRegistry = assetSourceRegistry,
            chainStateRepository = chainStateRepository
        )
    }
}

private const val SOURCE_ID = "AssetConversion"

private class AssetConversionExchange(
    private val chain: Chain,
    private val multiLocationConverter: MultiLocationConverter,
    private val remoteStorageSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val extrinsicService: ExtrinsicService,
    private val assetSourceRegistry: AssetSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
) : AssetExchange {

    override suspend fun sync() {
        // Nothing to sync
    }

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        // any asset is usable as a fee as soon as it has associated pool
        return true
    }

    override suspend fun availableSwapDirections(): MultiMap<FullChainAssetId, FullChainAssetId> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAllAvailableDirections(allPools)
        }
    }

    override suspend fun quote(args: AssetExchangeQuoteArgs): AssetExchangeQuote {
        val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)
        val quotedBalance = runtimeCallsApi.quote(
            swapDirection = args.swapDirection,
            assetIn = args.chainAssetIn,
            assetOut = args.chainAssetOut,
            amount = args.amount
        ) ?: throw SwapQuoteException.NotEnoughLiquidity

        val quotePath = QuotePath(
            segments = listOf(
                QuotePath.Segment(
                    from = args.chainAssetIn.fullId,
                    to = args.chainAssetOut.fullId,
                    sourceId = SOURCE_ID,
                    sourceParams = emptyMap()
                )
            )
        )

        return AssetExchangeQuote(quote = quotedBalance, path = quotePath, direction = args.swapDirection)
    }

    override suspend fun estimateFee(args: SwapExecuteArgs): AssetExchangeFee {
        val nativeAssetFee = extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            executeSwap(args, sendTo = chain.emptyAccountId())
        }

        return convertNativeFeeToPayingTokenFee(nativeAssetFee, args)
    }

    override suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission> {
        return extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet) { submissionOrigin ->
            // Send swapped funds to the requested origin since it the account doing the swap
            executeSwap(args, sendTo = submissionOrigin.requestedOrigin)
        }
    }

    override suspend fun slippageConfig(): SlippageConfig {
        return SlippageConfig.default()
    }

    override fun runSubscriptions(chain: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger> {
        return chainStateRepository.currentBlockNumberFlow(chain.id)
            .drop(1) // skip immediate value from the cache to not perform double-quote on chain change
            .map { ReQuoteTrigger }
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

    private suspend fun convertNativeFeeToPayingTokenFee(nativeTokenFee: Fee, args: SwapExecuteArgs): AssetExchangeFee {
        val customFeeAsset = args.customFeeAsset

        return if (customFeeAsset != null && !customFeeAsset.isCommissionAsset()) {
            calculateCustomTokenFee(nativeTokenFee, args.nativeAsset, customFeeAsset)
        } else {
            AssetExchangeFee(nativeTokenFee, MinimumBalanceBuyIn.NoBuyInNeeded)
        }
    }

    // TODO we purposefully do not use `nativeTokenFee.amountByRequestedAccount`
    // since we have disabled fee payment in custom tokens for accounts where the difference matters (e.g. proxy)
    // We should adapt it if we decide to remove the restriction
    private suspend fun calculateCustomTokenFee(
        nativeTokenFee: Fee,
        nativeAsset: Asset,
        customFeeAsset: Chain.Asset
    ): AssetExchangeFee {
        val nativeChainAsset = nativeAsset.token.configuration
        val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)
        val assetBalances = assetSourceRegistry.sourceFor(nativeChainAsset).balance

        val minimumBalance = assetBalances.existentialDeposit(chain, nativeChainAsset)
        // https://github.com/paritytech/polkadot-sdk/blob/39c04fdd9622792ba8478b1c1c300417943a034b/substrate/frame/transaction-payment/asset-conversion-tx-payment/src/payment.rs#L114
        val shouldBuyMinimumBalance = nativeAsset.balanceCountedTowardsEDInPlanks < minimumBalance + nativeTokenFee.amount

        val toBuyNativeFee = runtimeCallsApi.quoteFeeConversion(nativeTokenFee.amount, customFeeAsset)

        val minimumBalanceBuyIn = if (shouldBuyMinimumBalance) {
            val totalConverted = nativeTokenFee.amount + minimumBalance

            val forFeesAndMinBalance = runtimeCallsApi.quoteFeeConversion(totalConverted, customFeeAsset)
            val forMinBalance = forFeesAndMinBalance - toBuyNativeFee

            MinimumBalanceBuyIn.NeedsToBuyMinimumBalance(
                nativeAsset = nativeAsset.token.configuration,
                nativeMinimumBalance = minimumBalance,
                commissionAsset = customFeeAsset,
                commissionAssetToSpendOnBuyIn = forMinBalance
            )
        } else {
            MinimumBalanceBuyIn.NoBuyInNeeded
        }

        return AssetExchangeFee(
            networkFee = SubstrateFee(toBuyNativeFee, nativeTokenFee.submissionOrigin, asset = customFeeAsset),
            minimumBalanceBuyIn = minimumBalanceBuyIn
        )
    }

    private suspend fun RuntimeCallsApi.quoteFeeConversion(commissionAmountOut: Balance, customFeeToken: Chain.Asset): Balance {
        val quotedAmount = quote(
            swapDirection = SwapDirection.SPECIFIED_OUT,
            assetIn = customFeeToken,
            assetOut = chain.utilityAsset,
            amount = commissionAmountOut
        )

        return requireNotNull(quotedAmount)
    }

    private suspend fun ExtrinsicBuilder.executeSwap(swapExecuteArgs: SwapExecuteArgs, sendTo: AccountId) {
        val path = listOf(swapExecuteArgs.assetIn, swapExecuteArgs.assetOut)
            .map { asset -> multiLocationConverter.encodableMultiLocationOf(asset) }

        val keepAlive = false

        when (val swapLimit = swapExecuteArgs.swapLimit) {
            is SwapLimit.SpecifiedIn -> call(
                moduleName = Modules.ASSET_CONVERSION,
                callName = "swap_exact_tokens_for_tokens",
                arguments = mapOf(
                    "path" to path,
                    "amount_in" to swapLimit.expectedAmountIn,
                    "amount_out_min" to swapLimit.amountOutMin,
                    "send_to" to sendTo,
                    "keep_alive" to keepAlive
                )
            )

            is SwapLimit.SpecifiedOut -> call(
                moduleName = Modules.ASSET_CONVERSION,
                callName = "swap_tokens_for_exact_tokens",
                arguments = mapOf(
                    "path" to path,
                    "amount_out" to swapLimit.expectedAmountOut,
                    "amount_in_max" to swapLimit.amountInMax,
                    "send_to" to sendTo,
                    "keep_alive" to keepAlive
                )
            )
        }

        setFeeAsset(swapExecuteArgs.customFeeAsset)
    }

    private suspend fun ExtrinsicBuilder.setFeeAsset(feeAsset: Chain.Asset?) {
        if (feeAsset == null || feeAsset.isCommissionAsset()) return

        val assetId = multiLocationConverter.encodableMultiLocationOf(feeAsset)

        assetTxPayment(assetId)
    }

    private fun Chain.Asset.isCommissionAsset(): Boolean {
        return fullId == chain.commissionAsset.fullId
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
