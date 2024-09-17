package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.assetConversion
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationFee
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecutionCorrection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.AssetExchange
import io.novafoundation.nova.feature_swap_impl.domain.swap.BaseSwapGraphEdge
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.emptyAccountId
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions.assetTxPayment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
    private val chainStateRepository: ChainStateRepository,
) : AssetExchange.Factory {

    override suspend fun create(
        chain: Chain,
        parentQuoter: AssetExchange.ParentQuoter,
        coroutineScope: CoroutineScope
    ): AssetExchange {
        val converter = multiLocationConverterFactory.default(chain, coroutineScope)

        return AssetConversionExchange(
            chain = chain,
            multiLocationConverter = converter,
            remoteStorageSource = remoteStorageSource,
            multiChainRuntimeCallsApi = runtimeCallsApi,
            extrinsicService = extrinsicService,
            chainStateRepository = chainStateRepository
        )
    }
}

private class AssetConversionExchange(
    private val chain: Chain,
    private val multiLocationConverter: MultiLocationConverter,
    private val remoteStorageSource: StorageDataSource,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val extrinsicService: ExtrinsicService,
    private val chainStateRepository: ChainStateRepository,
) : AssetExchange {

    override suspend fun canPayFeeInNonUtilityToken(asset: Chain.Asset): Boolean {
        // any asset is usable as a fee as soon as it has associated pool
        return true
    }

    override suspend fun availableDirectSwapConnections(): List<SwapGraphEdge> {
        return remoteStorageSource.query(chain.id) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAllAvailableDirections(allPools)
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

    private suspend fun constructAllAvailableDirections(pools: List<Pair<MultiLocation, MultiLocation>>): List<AssetConversionEdge> {
        return buildList {
            pools.forEach { (firstLocation, secondLocation) ->
                val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@forEach
                val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@forEach

                add(AssetConversionEdge(firstAsset, secondAsset))
                add(AssetConversionEdge(secondAsset, firstAsset))
            }
        }
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

    private inner class AssetConversionEdge(fromAsset: Chain.Asset, toAsset: Chain.Asset) : BaseSwapGraphEdge(fromAsset, toAsset) {

        override suspend fun beginOperation(args: AtomicSwapOperationArgs): AtomicSwapOperation {
            return AssetConversionOperation(args, fromAsset, toAsset)
        }

        override suspend fun appendToOperation(currentTransaction: AtomicSwapOperation, args: AtomicSwapOperationArgs): AtomicSwapOperation? {
            return null
        }

        override suspend fun quote(
            amount: Balance,
            direction: SwapDirection
        ): Balance {
            val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)

            return runtimeCallsApi.quote(
                swapDirection = direction,
                assetIn = fromAsset,
                assetOut = toAsset,
                amount = amount
            ) ?: throw SwapQuoteException.NotEnoughLiquidity
        }
    }

    inner class AssetConversionOperation(
        private val transactionArgs: AtomicSwapOperationArgs,
        private val fromAsset: Chain.Asset,
        private val toAsset: Chain.Asset
    ) : AtomicSwapOperation {

        override suspend fun estimateFee(): AtomicSwapOperationFee {
            val nativeAssetFee = extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
                executeSwap(sendTo = chain.emptyAccountId())
            }

            return convertNativeFeeToPayingTokenFee(nativeAssetFee)
        }

        override suspend fun submit(previousStepCorrection: SwapExecutionCorrection?): Result<SwapExecutionCorrection> {
            // TODO use `previousStepCorrection` to correct used call arguments
            // TODO implement watching for extrinsic events
            return extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) { submissionOrigin ->
                // Send swapped funds to the requested origin since it the account doing the swap
                executeSwap(sendTo = submissionOrigin.requestedOrigin)
            }.awaitInBlock().map {
                SwapExecutionCorrection()
            }
        }

        private suspend fun convertNativeFeeToPayingTokenFee(nativeTokenFee: Fee): AtomicSwapOperationFee {
            val customFeeAsset = transactionArgs.customFeeAsset

            return if (customFeeAsset != null && !customFeeAsset.isCommissionAsset()) {
                calculateCustomTokenFee(nativeTokenFee, customFeeAsset)
            } else {
                nativeTokenFee
            }
        }

        private suspend fun ExtrinsicBuilder.executeSwap(sendTo: AccountId) {
            val path = listOf(fromAsset, toAsset)
                .map { asset -> multiLocationConverter.encodableMultiLocationOf(asset) }

            val keepAlive = false

            when (val swapLimit = transactionArgs.swapLimit) {
                is SwapLimit.SpecifiedIn -> call(
                    moduleName = Modules.ASSET_CONVERSION,
                    callName = "swap_exact_tokens_for_tokens",
                    arguments = mapOf(
                        "path" to path,
                        "amount_in" to swapLimit.amountIn,
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
                        "amount_out" to swapLimit.amountOut,
                        "amount_in_max" to swapLimit.amountInMax,
                        "send_to" to sendTo,
                        "keep_alive" to keepAlive
                    )
                )
            }

            setFeeAsset(transactionArgs.customFeeAsset)
        }

        private suspend fun ExtrinsicBuilder.setFeeAsset(feeAsset: Chain.Asset?) {
            if (feeAsset == null || feeAsset.isCommissionAsset()) return

            val assetId = multiLocationConverter.encodableMultiLocationOf(feeAsset)

            assetTxPayment(assetId)
        }

        // TODO we purposefully do not use `nativeTokenFee.amountByRequestedAccount`
        // since we have disabled fee payment in custom tokens for accounts where the difference matters (e.g. proxy)
        // We should adapt it if we decide to remove the restriction
        private suspend fun calculateCustomTokenFee(
            nativeTokenFee: Fee,
            customFeeAsset: Chain.Asset
        ): AtomicSwapOperationFee {
            val runtimeCallsApi = multiChainRuntimeCallsApi.forChain(chain.id)
            val toBuyNativeFee = runtimeCallsApi.quoteFeeConversion(nativeTokenFee.amount, customFeeAsset)

            return SubstrateFee(toBuyNativeFee, nativeTokenFee.submissionOrigin)
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

        private fun Chain.Asset.isCommissionAsset(): Boolean {
            return fullId == chain.commissionAsset.fullId
        }

        private suspend fun MultiLocationConverter.encodableMultiLocationOf(chainAsset: Chain.Asset): Any? {
            return toMultiLocationOrThrow(chainAsset).toEncodableInstance()
        }
    }
}
