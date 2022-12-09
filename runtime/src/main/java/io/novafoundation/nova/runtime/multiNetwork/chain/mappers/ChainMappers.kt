package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumberOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromJsonOrNull
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainTransferHistoryApiLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderArguments
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapSectionTypeToSectionTypeLocal(sectionType: Chain.ExternalApi.Section.Type): String = sectionType.name
private fun mapSectionTypeLocalToSectionType(sectionType: String): Chain.ExternalApi.Section.Type = enumValueOf(sectionType)

fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String = stakingType.name
private fun mapStakingTypeFromLocal(stakingTypeLocal: String): Chain.Asset.StakingType = enumValueOf(stakingTypeLocal)

private fun mapTransferApiAssetTypeFromLocal(localType: ChainTransferHistoryApiLocal.AssetType): Chain.ExternalApi.TransferHistoryApi.AssetType {
    return when (localType) {
        ChainTransferHistoryApiLocal.AssetType.SUBSTRATE -> Chain.ExternalApi.TransferHistoryApi.AssetType.SUBSTRATE
        ChainTransferHistoryApiLocal.AssetType.EVM -> Chain.ExternalApi.TransferHistoryApi.AssetType.EVM
        ChainTransferHistoryApiLocal.AssetType.UNSUPPORTED -> Chain.ExternalApi.TransferHistoryApi.AssetType.UNSUPPORTED
    }
}

private fun mapTransferApiTypeFromLocal(localType: ChainTransferHistoryApiLocal.ApiType): Chain.ExternalApi.Section.Type {
    return when (localType) {
        ChainTransferHistoryApiLocal.ApiType.SUBQUERY -> Chain.ExternalApi.Section.Type.SUBQUERY
        ChainTransferHistoryApiLocal.ApiType.GITHUB -> Chain.ExternalApi.Section.Type.GITHUB
        ChainTransferHistoryApiLocal.ApiType.POLKASSEMBLY -> Chain.ExternalApi.Section.Type.POLKASSEMBLY
        ChainTransferHistoryApiLocal.ApiType.ETHERSCAN -> Chain.ExternalApi.Section.Type.ETHERSCAN
        ChainTransferHistoryApiLocal.ApiType.UNKNOWN -> Chain.ExternalApi.Section.Type.UNKNOWN
    }
}

private fun mapAssetSourceFromLocal(source: AssetSourceLocal): Chain.Asset.Source {
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

private fun mapSectionLocalToSection(sectionLocal: ChainLocal.ExternalApi.Section?) = sectionLocal?.let {
    Chain.ExternalApi.Section(
        type = mapSectionTypeLocalToSectionType(sectionLocal.type),
        url = sectionLocal.url
    )
}

private const val ASSET_NATIVE = "native"
private const val ASSET_STATEMINE = "statemine"
private const val ASSET_ORML = "orml"
private const val ASSET_UNSUPPORTED = "unsupported"

private const val ASSET_EVM = "evm"

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
            val id = typeExtras?.get(STATEMINE_EXTRAS_ID)?.asGsonParsedNumberOrNull()
            val palletName = typeExtras?.get(STATEMINE_EXTRAS_PALLET_NAME) as String?

            Chain.Asset.Type.Statemine(id!!, palletName)
        }
        ASSET_ORML -> {
            Chain.Asset.Type.Orml(
                currencyIdScale = typeExtras!![ORML_EXTRAS_CURRENCY_ID_SCALE] as String,
                currencyIdType = typeExtras[ORML_EXTRAS_CURRENCY_TYPE] as String,
                existentialDeposit = (typeExtras[ORML_EXTRAS_EXISTENTIAL_DEPOSIT] as String).toBigInteger(),
                transfersEnabled = typeExtras[ORML_EXTRAS_TRANSFERS_ENABLED] as Boolean? ?: ORML_TRANSFERS_ENABLED_DEFAULT,
            )
        }
        ASSET_EVM -> {
            Chain.Asset.Type.Evm(
                contractAddress = typeExtras!![EVM_EXTRAS_CONTRACT_ADDRESS] as String
            )
        }
        else -> Chain.Asset.Type.Unsupported
    }
}

fun mapChainAssetTypeToRaw(type: Chain.Asset.Type): Pair<String, Map<String, Any?>?> = when (type) {
    is Chain.Asset.Type.Native -> ASSET_NATIVE to null
    is Chain.Asset.Type.Statemine -> ASSET_STATEMINE to mapOf(
        STATEMINE_EXTRAS_ID to type.id.toString(),
        STATEMINE_EXTRAS_PALLET_NAME to type.palletName
    )
    is Chain.Asset.Type.Orml -> ASSET_ORML to mapOf(
        ORML_EXTRAS_CURRENCY_ID_SCALE to type.currencyIdScale,
        ORML_EXTRAS_CURRENCY_TYPE to type.currencyIdType,
        ORML_EXTRAS_EXISTENTIAL_DEPOSIT to type.existentialDeposit.toString(),
        ORML_EXTRAS_TRANSFERS_ENABLED to type.transfersEnabled
    )
    is Chain.Asset.Type.Evm -> ASSET_EVM to mapOf(
        EVM_EXTRAS_CONTRACT_ADDRESS to type.contractAddress
    )

    Chain.Asset.Type.Unsupported -> ASSET_UNSUPPORTED to null
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
        staking = mapStakingTypeToLocal(asset.staking),
        type = type,
        source = mapAssetSourceToLocal(asset.source),
        buyProviders = gson.toJson(asset.buyProviders),
        typeExtras = gson.toJson(typeExtras),
        icon = asset.iconUrl,
        enabled = asset.enabled
    )
}

fun mapChainLocalToChain(chainLocal: JoinedChainInfo, gson: Gson): Chain {
    val nodes = chainLocal.getSortedNodes().map {
        Chain.Node(
            url = it.url,
            name = it.name,
            chainId = it.chainId,
            orderId = it.orderId
        )
    }

    val assets = chainLocal.assets.map {
        val typeExtrasParsed = it.typeExtras?.let(gson::parseArbitraryObject)
        val buyProviders = it.buyProviders?.let<String, Map<BuyProviderId, BuyProviderArguments>?>(gson::fromJsonOrNull).orEmpty()

        Chain.Asset(
            iconUrl = it.icon,
            id = it.id,
            symbol = it.symbol,
            precision = it.precision,
            name = it.name,
            chainId = it.chainId,
            priceId = it.priceId,
            buyProviders = buyProviders,
            staking = mapStakingTypeFromLocal(it.staking),
            type = mapChainAssetTypeFromRaw(it.type, typeExtrasParsed),
            source = mapAssetSourceFromLocal(it.source),
            enabled = it.enabled
        )
    }

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
            url = it.url,
            overridesCommon = it.overridesCommon
        )
    }

    val historyApis = chainLocal.transferHistoryApis.map {
        Chain.ExternalApi.TransferHistoryApi(
            assetType = mapTransferApiAssetTypeFromLocal(it.assetType),
            apiType = mapTransferApiTypeFromLocal(it.apiType),
            url = it.url
        )
    }

    val externalApiLocal = chainLocal.chain.externalApi
    val externalApi = if (externalApiLocal != null || historyApis.isNotEmpty()) {
        Chain.ExternalApi(
            staking = mapSectionLocalToSection(externalApiLocal?.staking),
            history = historyApis,
            crowdloans = mapSectionLocalToSection(externalApiLocal?.crowdloans),
            governance = mapSectionLocalToSection(externalApiLocal?.governance)
        )
    } else {
        null
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
            nodes = nodes,
            explorers = explorers,
            icon = icon,
            externalApi = externalApi,
            addressPrefix = prefix,
            isEthereumBased = isEthereumBased,
            isTestNet = isTestNet,
            hasCrowdloans = hasCrowdloans,
            governance = mapGovernanceListFromLocal(governance),
            additional = additional
        )
    }
}

private fun mapGovernanceListFromLocal(governanceLocal: String) = governanceLocal.split(",").mapNotNull {
    runCatching { Chain.Governance.valueOf(it) }.getOrNull()
}
