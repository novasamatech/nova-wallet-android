package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.Companion.EMPTY_CHAIN_ICON
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.ext.autoBalanceEnabled
import io.novafoundation.nova.runtime.ext.selectedUnformattedWssNodeUrlOrNull
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.EVM_TRANSFER_PARAMETER
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.GovernanceReferendaParameters
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.SUBSTRATE_TRANSFER_PARAMETER
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.TransferParameters
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ConnectionState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId

fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String = stakingType.name

fun mapStakingTypesToLocal(stakingTypes: List<Chain.Asset.StakingType>): String {
    return stakingTypes.joinToString(separator = ",", transform = ::mapStakingTypeToLocal)
}

private fun mapAssetSourceToLocal(source: Chain.Asset.Source): AssetSourceLocal {
    return when (source) {
        Chain.Asset.Source.DEFAULT -> AssetSourceLocal.DEFAULT
        Chain.Asset.Source.ERC20 -> AssetSourceLocal.ERC20
        Chain.Asset.Source.MANUAL -> AssetSourceLocal.MANUAL
    }
}

fun mapChainAssetTypeToRaw(type: Chain.Asset.Type): Pair<String, Map<String, Any?>?> = when (type) {
    is Chain.Asset.Type.Native -> ASSET_NATIVE to null

    is Chain.Asset.Type.Statemine -> ASSET_STATEMINE to mapOf(
        STATEMINE_EXTRAS_ID to mapStatemineAssetIdToRaw(type.id),
        STATEMINE_EXTRAS_PALLET_NAME to type.palletName,
        STATEMINE_IS_SUFFICIENT to type.isSufficient
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
        symbol = asset.symbol.value,
        precision = asset.precision.value,
        chainId = asset.chainId,
        name = asset.name,
        priceId = asset.priceId,
        staking = mapStakingTypesToLocal(asset.staking),
        type = type,
        source = mapAssetSourceToLocal(asset.source),
        buyProviders = gson.toJson(asset.buyProviders),
        typeExtras = gson.toJson(typeExtras),
        icon = asset.icon,
        enabled = asset.enabled
    )
}

fun mapChainToLocal(chain: Chain, gson: Gson): ChainLocal {
    val types = chain.types?.let {
        ChainLocal.TypesConfig(
            url = it.url.orEmpty(),
            overridesCommon = it.overridesCommon
        )
    }

    return ChainLocal(
        id = chain.id,
        parentId = chain.parentId,
        name = chain.name,
        icon = chain.icon ?: EMPTY_CHAIN_ICON,
        types = types,
        prefix = chain.addressPrefix,
        legacyPrefix = chain.legacyAddressPrefix,
        isEthereumBased = chain.isEthereumBased,
        isTestNet = chain.isTestNet,
        hasSubstrateRuntime = chain.hasSubstrateRuntime,
        pushSupport = chain.pushSupport,
        hasCrowdloans = chain.hasCrowdloans,
        supportProxy = chain.supportProxy,
        swap = mapSwapListToLocal(chain.swap),
        customFee = mapCustomFeeToLocal(chain.customFee),
        governance = mapGovernanceListToLocal(chain.governance),
        additional = chain.additional?.let { gson.toJson(it) },
        connectionState = mapConnectionStateToLocal(chain.connectionState),
        nodeSelectionStrategy = mapNodeSelectionStrategyToLocal(chain),
        source = mapChainSourceToLocal(chain.source)
    )
}

fun mapChainNodeToLocal(node: Chain.Node): ChainNodeLocal {
    return ChainNodeLocal(
        chainId = node.chainId,
        url = node.unformattedUrl,
        name = node.name,
        orderId = node.orderId,
        source = if (node.isCustom) ChainNodeLocal.Source.CUSTOM else ChainNodeLocal.Source.DEFAULT,
    )
}

fun mapChainExplorerToLocal(explorer: Chain.Explorer): ChainExplorerLocal {
    return ChainExplorerLocal(
        chainId = explorer.chainId,
        name = explorer.name,
        extrinsic = explorer.extrinsic,
        account = explorer.account,
        event = explorer.event,
    )
}

fun mapNodeSelectionPreferencesToLocal(chain: Chain): NodeSelectionPreferencesLocal {
    return NodeSelectionPreferencesLocal(
        chainId = chain.id,
        autoBalanceEnabled = chain.autoBalanceEnabled,
        selectedNodeUrl = chain.selectedUnformattedWssNodeUrlOrNull
    )
}

fun mapNodeSelectionStrategyToLocal(domain: Chain): ChainLocal.AutoBalanceStrategyLocal {
    return when (domain.nodes.autoBalanceStrategy) {
        Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN -> ChainLocal.AutoBalanceStrategyLocal.ROUND_ROBIN
        Chain.Nodes.AutoBalanceStrategy.UNIFORM -> ChainLocal.AutoBalanceStrategyLocal.UNIFORM
    }
}

fun mapChainSourceToLocal(domain: Chain.Source): ChainLocal.Source {
    return when (domain) {
        Chain.Source.CUSTOM -> ChainLocal.Source.CUSTOM
        Chain.Source.DEFAULT -> ChainLocal.Source.DEFAULT
    }
}

fun mapConnectionStateToLocal(domain: ConnectionState): ConnectionStateLocal {
    return when (domain) {
        ConnectionState.FULL_SYNC -> ConnectionStateLocal.FULL_SYNC
        ConnectionState.LIGHT_SYNC -> ConnectionStateLocal.LIGHT_SYNC
        ConnectionState.DISABLED -> ConnectionStateLocal.DISABLED
    }
}

fun mapChainExternalApiToLocal(gson: Gson, chainId: String, api: ExternalApi): ChainExternalApiLocal {
    return when (api) {
        is ExternalApi.Transfers -> mapExternalApiTransfers(gson, chainId, api)
        is ExternalApi.Crowdloans -> mapExternalApiCrowdloans(chainId, api)
        is ExternalApi.GovernanceDelegations -> mapExternalApiGovernanceDelegations(chainId, api)
        is ExternalApi.GovernanceReferenda -> mapExternalApiGovernanceReferenda(gson, chainId, api)
        is ExternalApi.Staking -> mapExternalApiStaking(chainId, api)
        is ExternalApi.ReferendumSummary -> mapExternalApiReferendumSummary(chainId, api)
        is ExternalApi.Multisig -> mapExternalApiMultisig(chainId, api)
    }
}

private fun mapExternalApiTransfers(gson: Gson, chainId: String, api: ExternalApi.Transfers): ChainExternalApiLocal {
    fun transferParametersByType(gson: Gson, type: String): String {
        return gson.toJson(TransferParameters(type))
    }

    val parameters = when (api) {
        is ExternalApi.Transfers.Evm -> transferParametersByType(gson, EVM_TRANSFER_PARAMETER)
        is ExternalApi.Transfers.Substrate -> transferParametersByType(gson, SUBSTRATE_TRANSFER_PARAMETER)
    }

    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.UNKNOWN,
        apiType = ChainExternalApiLocal.ApiType.TRANSFERS,
        parameters = parameters,
        url = api.url
    )
}

private fun mapExternalApiCrowdloans(chainId: String, api: ExternalApi.Crowdloans): ChainExternalApiLocal {
    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.GITHUB,
        apiType = ChainExternalApiLocal.ApiType.CROWDLOANS,
        parameters = null,
        url = api.url
    )
}

private fun mapExternalApiGovernanceDelegations(chainId: String, api: ExternalApi.GovernanceDelegations): ChainExternalApiLocal {
    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.SUBQUERY,
        apiType = ChainExternalApiLocal.ApiType.GOVERNANCE_DELEGATIONS,
        parameters = null,
        url = api.url
    )
}

private fun mapExternalApiGovernanceReferenda(gson: Gson, chainId: String, api: ExternalApi.GovernanceReferenda): ChainExternalApiLocal {
    val (source, parameters) = when (api.source) {
        ExternalApi.GovernanceReferenda.Source.SubSquare -> SourceType.SUBSQUARE to null

        is ExternalApi.GovernanceReferenda.Source.Polkassembly -> {
            SourceType.POLKASSEMBLY to gson.toJson(GovernanceReferendaParameters(api.source.network))
        }
    }

    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = source,
        apiType = ChainExternalApiLocal.ApiType.GOVERNANCE_REFERENDA,
        parameters = parameters,
        url = api.url
    )
}

private fun mapExternalApiStaking(chainId: String, api: ExternalApi.Staking): ChainExternalApiLocal {
    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.SUBQUERY,
        apiType = ChainExternalApiLocal.ApiType.STAKING,
        parameters = null,
        url = api.url
    )
}

private fun mapExternalApiReferendumSummary(chainId: String, api: ExternalApi.ReferendumSummary): ChainExternalApiLocal {
    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.UNKNOWN,
        apiType = ChainExternalApiLocal.ApiType.REFERENDUM_SUMMARY,
        parameters = null,
        url = api.url
    )
}

private fun mapExternalApiMultisig(chainId: String, api: ExternalApi.Multisig): ChainExternalApiLocal {
    return ChainExternalApiLocal(
        chainId = chainId,
        sourceType = SourceType.SUBSQUARE,
        apiType = ChainExternalApiLocal.ApiType.MULTISIG,
        parameters = null,
        url = api.url
    )
}
