package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromJsonOrNull
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.ApiType
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.NodeSelectionStrategyLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderArguments
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ConnectionState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId

fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String = stakingType.name

fun mapStakingTypesToLocal(stakingTypes: List<Chain.Asset.StakingType>): String {
    return stakingTypes.joinToString(separator = ",", transform = ::mapStakingTypeToLocal)
}

private fun mapStakingTypeFromLocal(stakingTypesLocal: String): List<Chain.Asset.StakingType> {
    if (stakingTypesLocal.isEmpty()) return emptyList()

    return stakingTypesLocal.split(",").map(Chain.Asset.StakingType::valueOf)
}

fun mapAssetSourceFromLocal(source: AssetSourceLocal): Chain.Asset.Source {
    return when (source) {
        AssetSourceLocal.DEFAULT -> Chain.Asset.Source.DEFAULT
        AssetSourceLocal.ERC20 -> Chain.Asset.Source.ERC20
        AssetSourceLocal.MANUAL -> Chain.Asset.Source.MANUAL
    }
}

private fun mapAssetSourceToLocal(source: Chain.Asset.Source): AssetSourceLocal {
    return when (source) {
        Chain.Asset.Source.DEFAULT -> AssetSourceLocal.DEFAULT
        Chain.Asset.Source.ERC20 -> AssetSourceLocal.ERC20
        Chain.Asset.Source.MANUAL -> AssetSourceLocal.MANUAL
    }
}

private const val ASSET_NATIVE = "native"
private const val ASSET_STATEMINE = "statemine"
private const val ASSET_ORML = "orml"
private const val ASSET_UNSUPPORTED = "unsupported"

private const val ASSET_EVM_ERC20 = "evm"
private const val ASSET_EVM_NATIVE = "evmNative"

private const val ASSET_EQUILIBRIUM = "equilibrium"
private const val ASSET_EQUILIBRIUM_ON_CHAIN_ID = "assetId"

private const val STATEMINE_EXTRAS_ID = "assetId"
private const val STATEMINE_EXTRAS_PALLET_NAME = "palletName"

private const val ORML_EXTRAS_CURRENCY_ID_SCALE = "currencyIdScale"
private const val ORML_EXTRAS_CURRENCY_TYPE = "currencyIdType"
private const val ORML_EXTRAS_EXISTENTIAL_DEPOSIT = "existentialDeposit"
private const val ORML_EXTRAS_TRANSFERS_ENABLED = "transfersEnabled"

private const val EVM_EXTRAS_CONTRACT_ADDRESS = "contractAddress"

private const val ORML_TRANSFERS_ENABLED_DEFAULT = true

private inline fun unsupportedOnError(creator: () -> Chain.Asset.Type): Chain.Asset.Type {
    return runCatching(creator)
        .onFailure { Log.e("ChainMapper", "Failed to construct chain type", it) }
        .getOrDefault(Chain.Asset.Type.Unsupported)
}

private fun mapChainAssetTypeFromRaw(type: String?, typeExtras: Map<String, Any?>?): Chain.Asset.Type = unsupportedOnError {
    when (type) {
        null, ASSET_NATIVE -> Chain.Asset.Type.Native

        ASSET_STATEMINE -> {
            val idRaw = typeExtras?.get(STATEMINE_EXTRAS_ID)!!
            val id = mapStatemineAssetIdFromRaw(idRaw)
            val palletName = typeExtras[STATEMINE_EXTRAS_PALLET_NAME] as String?

            Chain.Asset.Type.Statemine(id, palletName)
        }

        ASSET_ORML -> {
            Chain.Asset.Type.Orml(
                currencyIdScale = typeExtras!![ORML_EXTRAS_CURRENCY_ID_SCALE] as String,
                currencyIdType = typeExtras[ORML_EXTRAS_CURRENCY_TYPE] as String,
                existentialDeposit = (typeExtras[ORML_EXTRAS_EXISTENTIAL_DEPOSIT] as String).toBigInteger(),
                transfersEnabled = typeExtras[ORML_EXTRAS_TRANSFERS_ENABLED] as Boolean? ?: ORML_TRANSFERS_ENABLED_DEFAULT,
            )
        }

        ASSET_EVM_ERC20 -> {
            Chain.Asset.Type.EvmErc20(
                contractAddress = typeExtras!![EVM_EXTRAS_CONTRACT_ADDRESS] as String
            )
        }

        ASSET_EVM_NATIVE -> Chain.Asset.Type.EvmNative

        ASSET_EQUILIBRIUM -> Chain.Asset.Type.Equilibrium((typeExtras!![ASSET_EQUILIBRIUM_ON_CHAIN_ID] as String).toBigInteger())

        else -> Chain.Asset.Type.Unsupported
    }
}

fun mapChainAssetTypeToRaw(type: Chain.Asset.Type): Pair<String, Map<String, Any?>?> = when (type) {
    is Chain.Asset.Type.Native -> ASSET_NATIVE to null

    is Chain.Asset.Type.Statemine -> ASSET_STATEMINE to mapOf(
        STATEMINE_EXTRAS_ID to mapStatemineAssetIdToRaw(type.id),
        STATEMINE_EXTRAS_PALLET_NAME to type.palletName
    )

    is Chain.Asset.Type.Orml -> ASSET_ORML to mapOf(
        ORML_EXTRAS_CURRENCY_ID_SCALE to type.currencyIdScale,
        ORML_EXTRAS_CURRENCY_TYPE to type.currencyIdType,
        ORML_EXTRAS_EXISTENTIAL_DEPOSIT to type.existentialDeposit.toString(),
        ORML_EXTRAS_TRANSFERS_ENABLED to type.transfersEnabled
    )

    is Chain.Asset.Type.EvmErc20 -> ASSET_EVM_ERC20 to mapOf(
        EVM_EXTRAS_CONTRACT_ADDRESS to type.contractAddress
    )

    is Chain.Asset.Type.EvmNative -> ASSET_EVM_NATIVE to null

    is Chain.Asset.Type.Equilibrium -> ASSET_EQUILIBRIUM to mapOf(
        ASSET_EQUILIBRIUM_ON_CHAIN_ID to type.id.toString()
    )

    Chain.Asset.Type.Unsupported -> ASSET_UNSUPPORTED to null
}

private fun mapStatemineAssetIdToRaw(statemineAssetId: StatemineAssetId): String {
    return when (statemineAssetId) {
        is StatemineAssetId.Number -> statemineAssetId.value.toString()
        is StatemineAssetId.ScaleEncoded -> statemineAssetId.scaleHex
    }
}

fun mapStatemineAssetIdFromRaw(rawValue: Any): StatemineAssetId {
    val asString = rawValue as? String ?: error("Invalid format")

    return if (asString.startsWith("0x")) {
        StatemineAssetId.ScaleEncoded(asString)
    } else {
        StatemineAssetId.Number(asString.asGsonParsedNumber())
    }
}

fun mapChainAssetToLocal(asset: Chain.Asset, gson: Gson): ChainAssetLocal {
    val (type, typeExtras) = mapChainAssetTypeToRaw(asset.type)

    return ChainAssetLocal(
        id = asset.id,
        symbol = asset.symbol,
        precision = asset.precision,
        chainId = asset.chainId,
        name = asset.name,
        priceId = asset.priceId,
        staking = mapStakingTypesToLocal(asset.staking),
        type = type,
        source = mapAssetSourceToLocal(asset.source),
        buyProviders = gson.toJson(asset.buyProviders),
        typeExtras = gson.toJson(typeExtras),
        icon = asset.iconUrl,
        enabled = asset.enabled
    )
}

private fun <T> ChainExternalApiLocal.ensureSourceType(
    type: SourceType,
    action: ChainExternalApiLocal.() -> T
): T? {
    return if (sourceType == type) {
        action()
    } else {
        null
    }
}

private inline fun <reified T> ChainExternalApiLocal.parsedParameters(gson: Gson): T? {
    return parameters?.let { gson.fromJson<T>(it) }
}

private class TransferParameters(val assetType: String?)

private fun mapTransferApiFromLocal(local: ChainExternalApiLocal, gson: Gson): ExternalApi.Transfers? {
    val parameters = local.parsedParameters<TransferParameters>(gson)

    return when (parameters?.assetType) {
        null, "substrate" -> ExternalApi.Transfers.Substrate(local.url)
        "evm" -> ExternalApi.Transfers.Evm(local.url)
        else -> null
    }
}

private class GovernanceReferendaParameters(val network: String?)

private fun mapGovernanceReferendaApiFromLocal(local: ChainExternalApiLocal, gson: Gson): ExternalApi.GovernanceReferenda? {
    val source = when (local.sourceType) {
        SourceType.SUBSQUARE -> ExternalApi.GovernanceReferenda.Source.SubSquare

        SourceType.POLKASSEMBLY -> {
            val parameters = local.parsedParameters<GovernanceReferendaParameters>(gson)
            ExternalApi.GovernanceReferenda.Source.Polkassembly(parameters?.network)
        }

        else -> return null
    }

    return ExternalApi.GovernanceReferenda(local.url, source)
}

private fun mapExternalApiLocalToExternalApi(externalApiLocal: ChainExternalApiLocal, gson: Gson): ExternalApi? = runCatching {
    when (externalApiLocal.apiType) {
        ApiType.STAKING -> externalApiLocal.ensureSourceType(SourceType.SUBQUERY) {
            ExternalApi.Staking(externalApiLocal.url)
        }

        ApiType.CROWDLOANS -> externalApiLocal.ensureSourceType(SourceType.GITHUB) {
            ExternalApi.Crowdloans(externalApiLocal.url)
        }

        ApiType.TRANSFERS -> mapTransferApiFromLocal(externalApiLocal, gson)

        ApiType.GOVERNANCE_REFERENDA -> mapGovernanceReferendaApiFromLocal(externalApiLocal, gson)

        ApiType.GOVERNANCE_DELEGATIONS -> externalApiLocal.ensureSourceType(SourceType.SUBQUERY) {
            ExternalApi.GovernanceDelegations(externalApiLocal.url)
        }

        ApiType.UNKNOWN -> null
    }
}.getOrNull()

private fun mapNodeSelectionFromLocal(local: NodeSelectionStrategyLocal): NodeSelectionStrategy {
    return when (local) {
        NodeSelectionStrategyLocal.ROUND_ROBIN -> NodeSelectionStrategy.ROUND_ROBIN
        NodeSelectionStrategyLocal.UNIFORM -> NodeSelectionStrategy.UNIFORM
        NodeSelectionStrategyLocal.UNKNOWN -> NodeSelectionStrategy.ROUND_ROBIN
    }
}

fun mapChainLocalToChain(chainLocal: JoinedChainInfo, gson: Gson): Chain {
    val nodes = chainLocal.getSortedNodes().map {
        Chain.Node(
            unformattedUrl = it.url,
            name = it.name,
            chainId = it.chainId,
            orderId = it.orderId
        )
    }
    val nodesConfig = Chain.Nodes(
        nodeSelectionStrategy = mapNodeSelectionFromLocal(chainLocal.chain.nodeSelectionStrategy),
        nodes = nodes
    )

    val assets = chainLocal.assets.map { mapChainAssetLocalToAsset(it, gson) }

    val explorers = chainLocal.explorers.map {
        Chain.Explorer(
            name = it.name,
            account = it.account,
            extrinsic = it.extrinsic,
            event = it.event,
            chainId = it.chainId
        )
    }

    val types = chainLocal.chain.types?.let {
        Chain.Types(
            url = it.url.nullIfEmpty(),
            overridesCommon = it.overridesCommon
        )
    }

    val externalApis = chainLocal.externalApis.mapNotNull {
        mapExternalApiLocalToExternalApi(it, gson)
    }

    val additional = chainLocal.chain.additional?.let { raw ->
        gson.fromJson<Chain.Additional>(raw)
    }

    return with(chainLocal.chain) {
        Chain(
            id = id,
            parentId = parentId,
            name = name,
            assets = assets,
            types = types,
            nodes = nodesConfig,
            explorers = explorers,
            icon = icon,
            externalApis = externalApis,
            addressPrefix = prefix,
            isEthereumBased = isEthereumBased,
            isTestNet = isTestNet,
            hasCrowdloans = hasCrowdloans,
            pushSupport = pushSupport,
            supportProxy = supportProxy,
            hasSubstrateRuntime = hasSubstrateRuntime,
            governance = mapGovernanceListFromLocal(governance),
            swap = mapSwapListFromLocal(swap),
            connectionState = mapConnectionStateFromLocal(connectionState),
            additional = additional
        )
    }
}

fun mapChainAssetLocalToAsset(local: ChainAssetLocal, gson: Gson): Chain.Asset {
    val typeExtrasParsed = local.typeExtras?.let(gson::parseArbitraryObject)
    val buyProviders = local.buyProviders?.let<String, Map<BuyProviderId, BuyProviderArguments>?>(gson::fromJsonOrNull).orEmpty()

    return Chain.Asset(
        iconUrl = local.icon,
        id = local.id,
        symbol = local.symbol,
        precision = local.precision,
        name = local.name,
        chainId = local.chainId,
        priceId = local.priceId,
        buyProviders = buyProviders,
        staking = mapStakingTypeFromLocal(local.staking),
        type = mapChainAssetTypeFromRaw(local.type, typeExtrasParsed),
        source = mapAssetSourceFromLocal(local.source),
        enabled = local.enabled
    )
}

fun mapConnectionStateToLocal(domain: ConnectionState): ConnectionStateLocal {
    return when (domain) {
        ConnectionState.FULL_SYNC -> ConnectionStateLocal.FULL_SYNC
        ConnectionState.LIGHT_SYNC -> ConnectionStateLocal.LIGHT_SYNC
        ConnectionState.DISABLED -> ConnectionStateLocal.DISABLED
    }
}

private fun mapConnectionStateFromLocal(local: ConnectionStateLocal): ConnectionState {
    return when (local) {
        ConnectionStateLocal.FULL_SYNC -> ConnectionState.FULL_SYNC
        ConnectionStateLocal.LIGHT_SYNC -> ConnectionState.LIGHT_SYNC
        ConnectionStateLocal.DISABLED -> ConnectionState.DISABLED
    }
}

private fun mapGovernanceListFromLocal(governanceLocal: String) = governanceLocal.split(",").mapNotNull {
    runCatching { Chain.Governance.valueOf(it) }.getOrNull()
}

private fun mapSwapListFromLocal(swapLocal: String): List<Chain.Swap> {
    if (swapLocal.isEmpty()) return emptyList()

    return swapLocal.split(",").mapNotNull {
        enumValueOfOrNull<Chain.Swap>(swapLocal)
    }
}
