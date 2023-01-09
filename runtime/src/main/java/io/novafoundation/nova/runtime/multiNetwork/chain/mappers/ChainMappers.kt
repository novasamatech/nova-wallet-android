package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumberOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromJsonOrNull
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.ApiType
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderArguments
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ExternalApi

fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String = stakingType.name
private fun mapStakingTypeFromLocal(stakingTypeLocal: String): Chain.Asset.StakingType = enumValueOf(stakingTypeLocal)

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
    return local.ensureSourceType(SourceType.POLKASSEMBLY) {
        val parameters = local.parsedParameters<GovernanceReferendaParameters>(gson)
        val source = ExternalApi.GovernanceReferenda.Source.Polkassembly(parameters?.network)

        ExternalApi.GovernanceReferenda(local.url, source)
    }
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

        ApiType.UNKNOWN -> null
    }
}.getOrNull()

fun mapChainLocalToChain(chainLocal: JoinedChainInfo, gson: Gson): Chain {
    val nodes = chainLocal.getSortedNodes().map {
        Chain.Node(
            url = it.url,
            name = it.name,
            chainId = it.chainId,
            orderId = it.orderId
        )
    }

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
            url = it.url,
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
            nodes = nodes,
            explorers = explorers,
            icon = icon,
            externalApis = externalApis,
            addressPrefix = prefix,
            isEthereumBased = isEthereumBased,
            isTestNet = isTestNet,
            hasCrowdloans = hasCrowdloans,
            governance = mapGovernanceListFromLocal(governance),
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

private fun mapGovernanceListFromLocal(governanceLocal: String) = governanceLocal.split(",").mapNotNull {
    runCatching { Chain.Governance.valueOf(it) }.getOrNull()
}
