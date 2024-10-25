package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.utils.enumValueOfOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromJsonOrNull
import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.ApiType
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.AutoBalanceStrategyLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.BuildConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.EVM_TRANSFER_PARAMETER
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.GovernanceReferendaParameters
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.SUBSTRATE_TRANSFER_PARAMETER
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.utils.TransferParameters
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderArguments
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ConnectionState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.AutoBalanceStrategy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId

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

private fun mapStatemineAssetIdToRaw(statemineAssetId: StatemineAssetId): String {
    return when (statemineAssetId) {
        is StatemineAssetId.Number -> statemineAssetId.value.toString()
        is StatemineAssetId.ScaleEncoded -> statemineAssetId.scaleHex
    }
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

private fun mapTransferApiFromLocal(local: ChainExternalApiLocal, gson: Gson): ExternalApi.Transfers? {
    val parameters = local.parsedParameters<TransferParameters>(gson)

    return when (parameters?.assetType) {
        null, SUBSTRATE_TRANSFER_PARAMETER -> ExternalApi.Transfers.Substrate(local.url)
        EVM_TRANSFER_PARAMETER -> ExternalApi.Transfers.Evm(local.url)
        else -> null
    }
}

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

        ApiType.REFERENDUM_SUMMARY -> {
            ExternalApi.ReferendumSummary(externalApiLocal.url)
        }

        ApiType.UNKNOWN -> null
    }
}.getOrNull()

private fun mapAutoBalanceStrategyFromLocal(local: AutoBalanceStrategyLocal): AutoBalanceStrategy {
    return when (local) {
        AutoBalanceStrategyLocal.ROUND_ROBIN -> AutoBalanceStrategy.ROUND_ROBIN
        AutoBalanceStrategyLocal.UNIFORM -> AutoBalanceStrategy.UNIFORM
        AutoBalanceStrategyLocal.UNKNOWN -> AutoBalanceStrategy.ROUND_ROBIN
    }
}

private fun mapNodeSelectionFromLocal(nodeSelectionPreferencesLocal: NodeSelectionPreferencesLocal?): NodeSelectionStrategy {
    if (nodeSelectionPreferencesLocal == null) return NodeSelectionStrategy.AutoBalance

    val selectedUnformattedWssUrl = nodeSelectionPreferencesLocal.selectedUnformattedWssNodeUrl

    return if (selectedUnformattedWssUrl != null && !nodeSelectionPreferencesLocal.autoBalanceEnabled) {
        NodeSelectionStrategy.SelectedNode(selectedUnformattedWssUrl)
    } else {
        NodeSelectionStrategy.AutoBalance
    }
}

fun mapChainLocalToChain(chainLocal: JoinedChainInfo, gson: Gson, assetIconMode: AssetIconMode): Chain {
    return mapChainLocalToChain(
        chainLocal.chain,
        chainLocal.nodes,
        chainLocal.nodeSelectionPreferences,
        chainLocal.assets,
        chainLocal.explorers,
        chainLocal.externalApis,
        gson,
        assetIconMode
    )
}

fun mapChainLocalToChain(
    chainLocal: ChainLocal,
    nodesLocal: List<ChainNodeLocal>,
    nodeSelectionPreferences: NodeSelectionPreferencesLocal?,
    assetsLocal: List<ChainAssetLocal>,
    explorersLocal: List<ChainExplorerLocal>,
    externalApisLocal: List<ChainExternalApiLocal>,
    gson: Gson,
    assetIconMode: AssetIconMode
): Chain {
    val nodes = nodesLocal.sortedBy { it.orderId }.map {
        Chain.Node(
            unformattedUrl = it.url,
            name = it.name,
            chainId = it.chainId,
            orderId = it.orderId,
            isCustom = it.source == ChainNodeLocal.Source.CUSTOM,
        )
    }

    val nodesConfig = Chain.Nodes(
        autoBalanceStrategy = mapAutoBalanceStrategyFromLocal(chainLocal.autoBalanceStrategy),
        wssNodeSelectionStrategy = mapNodeSelectionFromLocal(nodeSelectionPreferences),
        nodes = nodes
    )

    val assets = assetsLocal.map { mapChainAssetLocalToAsset(it, gson, assetIconMode) }

    val explorers = explorersLocal.map {
        Chain.Explorer(
            name = it.name,
            account = it.account,
            extrinsic = it.extrinsic,
            event = it.event,
            chainId = it.chainId
        )
    }

    val types = chainLocal.types?.let {
        Chain.Types(
            url = it.url.nullIfEmpty(),
            overridesCommon = it.overridesCommon
        )
    }

    val externalApis = externalApisLocal.mapNotNull {
        mapExternalApiLocalToExternalApi(it, gson)
    }

    val additional = chainLocal.additional?.let { raw ->
        gson.fromJson<Chain.Additional>(raw)
    }

    return with(chainLocal) {
        Chain(
            id = id,
            parentId = parentId,
            name = name,
            assets = assets,
            types = types,
            nodes = nodesConfig,
            explorers = explorers,
            icon = icon.takeIf { it.isNotBlank() },
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
            customFee = mapCustomFeeFromLocal(customFee),
            connectionState = mapConnectionStateFromLocal(connectionState),
            additional = additional,
            source = mapSourceFromLocal(source)
        )
    }
}

fun mapChainAssetLocalToAsset(local: ChainAssetLocal, gson: Gson, assetIconMode: AssetIconMode): Chain.Asset {
    val typeExtrasParsed = local.typeExtras?.let(gson::parseArbitraryObject)
    val buyProviders = local.buyProviders?.let<String, Map<BuyProviderId, BuyProviderArguments>?>(gson::fromJsonOrNull).orEmpty()

    return Chain.Asset(
        icon = getAssetIconUrl(local.icon, assetIconMode),
        id = local.id,
        symbol = local.symbol.asTokenSymbol(),
        precision = local.precision.asPrecision(),
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

private fun getAssetIconUrl(icon: String?, assetIconMode: AssetIconMode): Chain.Icon? {
    return icon?.let {
        val baseUrl = when (assetIconMode) {
            AssetIconMode.COLORED -> BuildConfig.ASSET_COLORED_ICON_URL
            AssetIconMode.WHITE -> BuildConfig.ASSET_WHITE_ICON_URL
        }

        Chain.Icon(it, baseUrl)
    }
}

private fun mapSourceFromLocal(local: ChainLocal.Source): Chain.Source {
    return when (local) {
        ChainLocal.Source.DEFAULT -> Chain.Source.DEFAULT
        ChainLocal.Source.CUSTOM -> Chain.Source.CUSTOM
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

private fun mapCustomFeeFromLocal(customFee: String): List<Chain.CustomFee> {
    if (customFee.isEmpty()) return emptyList()

    return customFee.split(",").mapNotNull {
        enumValueOfOrNull<Chain.CustomFee>(it)
    }
}
