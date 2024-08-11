package io.novafoundation.nova.feature_account_impl.data.fee.types

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrNull
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.structOf
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.assetConversionOrNull
import io.novafoundation.nova.feature_account_api.data.conversion.assethub.pools
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.palletNameOrDefault
import io.novafoundation.nova.runtime.ext.requireStatemine
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.asNumberOrThrow
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.Junctions
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.MultiLocationConverterFactory
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.toEncodableInstance
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.BooleanType
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.metadata.RuntimeMetadata
import io.novasama.substrate_sdk_android.runtime.metadata.SignedExtensionValue
import io.novasama.substrate_sdk_android.runtime.metadata.call
import io.novasama.substrate_sdk_android.runtime.metadata.module
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope

internal class AssetConversionFeePayment(
    private val paymentAsset: Chain.Asset,
    private val chainRegistry: ChainRegistry,
    private val multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    private val remoteStorageSource: StorageDataSource,
    private val multiLocationConverterFactory: MultiLocationConverterFactory,
    private val coroutineScope: CoroutineScope
) : FeePayment {

    override suspend fun modifyExtrinsic(extrinsicBuilder: ExtrinsicBuilder) {
        return extrinsicBuilder.assetTxPayment(encodableAssetId())
    }

    override suspend fun convertNativeFee(nativeFee: Fee): Fee {
        val quote = multiChainRuntimeCallsApi.forChain(paymentAsset.chainId).convertNativeFee(nativeFee.amount)
        requireNotNull(quote) {
            Log.e(LOG_TAG, "Quote for ${paymentAsset.symbol} fee was null")

            "Failed to calculate fee in ${paymentAsset.symbol}"
        }

        return SubstrateFee(amount = quote, submissionOrigin = nativeFee.submissionOrigin, assetId = paymentAsset.fullId)
    }

    override suspend fun availableCustomFeeAssets(): List<Chain.Asset> {
        return remoteStorageSource.query(paymentAsset.chainId) {
            val allPools = metadata.assetConversionOrNull?.pools?.keys().orEmpty()

            constructAvailableCustomFeeAssets(allPools)
        }
    }

    private suspend fun constructAvailableCustomFeeAssets(pools: List<Pair<MultiLocation, MultiLocation>>): List<Chain.Asset> {
        val chain = chainRegistry.getChain(paymentAsset.chainId)
        val multiLocationConverter = multiLocationConverterFactory.default(chain, coroutineScope)
        return pools.mapNotNull { (firstLocation, secondLocation) ->
            val firstAsset = multiLocationConverter.toChainAsset(firstLocation) ?: return@mapNotNull null
            val secondAsset = multiLocationConverter.toChainAsset(secondLocation) ?: return@mapNotNull null

            if (!firstAsset.isUtilityAsset) return@mapNotNull null

            secondAsset
        }
    }

    private suspend fun encodableAssetId(): Any {
        val runtime = chainRegistry.getRuntime(paymentAsset.chainId)
        val assetsType = paymentAsset.requireStatemine()
        val index = assetsType.id.asNumberOrThrow()

        val pallet = runtime.metadata.module(assetsType.palletNameOrDefault())

        val multiLocation = MultiLocation(
            parents = BigInteger.ZERO,
            interior = Junctions(
                MultiLocation.Junction.PalletInstance(pallet.index),
                MultiLocation.Junction.GeneralIndex(index)
            )
        )

        return multiLocation.toEncodableInstance()
    }

    private fun encodableNativeAssetId(): Any {
        return MultiLocation(
            parents = BigInteger.ONE,
            interior = MultiLocation.Interior.Here
        ).toEncodableInstance()
    }

    private fun ExtrinsicBuilder.assetTxPayment(assetId: Any?, tip: BigInteger = BigInteger.ZERO) {
        val extensionValue = assetTxPaymentPayload(assetId, tip)

        signedExtension(
            id = "ChargeAssetTxPayment",
            value = SignedExtensionValue(includedInExtrinsic = extensionValue)
        )
    }

    private fun assetTxPaymentPayload(assetId: Any?, tip: BigInteger = BigInteger.ZERO): Any {
        return structOf(
            "tip" to tip,
            "assetId" to assetId
        )
    }

    private suspend fun RuntimeCallsApi.convertNativeFee(amount: BigInteger): BigInteger? {
        val method = "quote_price_tokens_for_exact_tokens"

        val includeFee = true

        val multiLocationTypeName = runtime.metadata.assetIdTypeName()

        return call(
            section = "AssetConversionApi",
            method = method,
            arguments = listOf(
                encodableAssetId() to multiLocationTypeName,
                encodableNativeAssetId() to multiLocationTypeName,
                amount to "Balance",
                includeFee to BooleanType.name
            ),
            returnType = "Option<Balance>",
            returnBinding = ::bindNumberOrNull
        )
    }

    private fun RuntimeMetadata.assetIdTypeName(): String {
        val (assetIdArgument) = module(Modules.ASSET_CONVERSION).call("add_liquidity").arguments

        val assetIdType = assetIdArgument.type!!

        return assetIdType.name
    }
}
