package io.novafoundation.nova.runtime.state

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.formatting.Formatable
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainWithAssetOrNull
import io.novafoundation.nova.runtime.state.GenericSingleAssetSharedState.SupportedAssetOption
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val DELIMITER = ":"

typealias SupportedOptionsResolver<A> = (Chain, Chain.Asset) -> List<A>

typealias SingleAssetSharedState = GenericSingleAssetSharedState<*>

interface AssetSharedStateAdditionalData : Formatable, Identifiable

abstract class GenericSingleAssetSharedState<A : AssetSharedStateAdditionalData>(
    private val preferencesKey: String,
    private val chainRegistry: ChainRegistry,
    private val supportedOptions: SupportedOptionsResolver<A>,
    private val preferences: Preferences
) : ChainIdHolder {

    data class SupportedAssetOption<A : AssetSharedStateAdditionalData>(
        val assetWithChain: ChainWithAsset,
        val additional: A
    )

    val selectedOption: Flow<SupportedAssetOption<A>> = preferences.stringFlow(
        field = preferencesKey,
        initialValueProducer = {
            val option = availableToSelect().first()
            val chainAsset = option.assetWithChain.asset
            val additional = option.additional

            encode(chainAsset.chainId, chainAsset.id, additional.identifier)
        }
    )
        .filterNotNull()
        .map { encoded ->
            val (chainId, chainAssetId, additionalIdentifier) = decode(encoded)

            getChainWithAssetOrFallback(chainId, chainAssetId, additionalIdentifier)
        }
        .inBackground()
        .shareIn(GlobalScope, started = SharingStarted.Eagerly, replay = 1)

    val assetWithChain: Flow<ChainWithAsset> = selectedOption.map { it.assetWithChain }
        .inBackground()
        .shareIn(GlobalScope, started = SharingStarted.Eagerly, replay = 1)

    suspend fun availableToSelect(): List<SupportedAssetOption<A>> {
        val allChains = chainRegistry.currentChains.first()

        return allChains.flatMap { chain ->
            chain.assets.flatMap { chainAsset ->
                supportedOptions(chain, chainAsset).map { additional ->
                    SupportedAssetOption(
                        assetWithChain = ChainWithAsset(chain = chain, asset = chainAsset),
                        additional = additional
                    )
                }
            }
        }
    }

    fun update(chainId: ChainId, chainAssetId: Int, optionIdentifier: String) {
        preferences.putString(preferencesKey, encode(chainId, chainAssetId, optionIdentifier))
    }

    override suspend fun chainId(): String {
        return assetWithChain.first().chain.id
    }

    private suspend fun getChainWithAssetOrFallback(chainId: ChainId, chainAssetId: Int, additionalIdentifier: String?): SupportedAssetOption<A> {
        val optionalChainAndAsset = chainRegistry.chainWithAssetOrNull(chainId, chainAssetId)
        val supportedOptions = optionalChainAndAsset?.let {
            supportedOptions(it.chain, it.asset)
        }.orEmpty()

        return when {
            // previously used chain asset was removed -> fallback to default
            optionalChainAndAsset == null -> availableToSelect().first()

            // previously supported option is no longer supported -> fallback to default
            supportedOptions.isEmpty() -> availableToSelect().first()

            // there is no particular additional option specified -> select first one
            additionalIdentifier == null -> SupportedAssetOption(optionalChainAndAsset, additional = supportedOptions.first())

            else -> {
                val option = supportedOptions.findById(additionalIdentifier) ?: supportedOptions.first()

                SupportedAssetOption(optionalChainAndAsset, additional = option)
            }
        }
    }

    private fun encode(chainId: ChainId, chainAssetId: Int, additionalIdentifier: String?): String {
        val additionalEncoded = additionalIdentifier.orEmpty()

        return "$chainId$DELIMITER$chainAssetId$DELIMITER$additionalEncoded"
    }

    private fun decode(value: String): Triple<ChainId, Int, String?> {
        val valueComponents = value.split(DELIMITER)
        val (chainId, chainAssetRaw) = valueComponents
        val additionalIdentifierRaw = valueComponents.getOrNull(2)

        return Triple(chainId, chainAssetRaw.toInt(), additionalIdentifierRaw)
    }
}

fun SingleAssetSharedState.selectedChainFlow() = assetWithChain
    .map { it.chain }
    .distinctUntilChanged()

suspend fun SingleAssetSharedState.chain() = assetWithChain.first().chain

suspend fun SingleAssetSharedState.chainAsset() = assetWithChain.first().asset

suspend fun SingleAssetSharedState.chainAndAsset() = assetWithChain.first()

suspend fun <A : AssetSharedStateAdditionalData> GenericSingleAssetSharedState<A>.selectedOption(): SupportedAssetOption<A> {
    return selectedOption.first()
}

fun SingleAssetSharedState.selectedAssetFlow() = assetWithChain
    .map { it.asset }
