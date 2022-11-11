package io.novafoundation.nova.runtime.multiNetwork.chain

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumberOrNull
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.common.utils.fromJsonOrNull
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderArguments
import io.novafoundation.nova.runtime.multiNetwork.chain.model.BuyProviderId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainExternalApiRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote

private const val ETHEREUM_OPTION = "ethereumBased"
private const val CROWDLOAN_OPTION = "crowdloans"
private const val TESTNET_OPTION = "testnet"

private fun mapSectionTypeRemoteToSectionType(section: String) = when (section) {
    "subquery" -> Chain.ExternalApi.Section.Type.SUBQUERY
    "github" -> Chain.ExternalApi.Section.Type.GITHUB
    "polkassembly" -> Chain.ExternalApi.Section.Type.POLKASSEMBLY
    else -> Chain.ExternalApi.Section.Type.UNKNOWN
}

private fun mapSectionTypeToSectionTypeLocal(sectionType: Chain.ExternalApi.Section.Type): String = sectionType.name
private fun mapSectionTypeLocalToSectionType(sectionType: String): Chain.ExternalApi.Section.Type = enumValueOf(sectionType)

private fun mapStakingStringToStakingType(stakingString: String?): Chain.Asset.StakingType {
    return when (stakingString) {
        null -> Chain.Asset.StakingType.UNSUPPORTED
        "relaychain" -> Chain.Asset.StakingType.RELAYCHAIN
        "parachain" -> Chain.Asset.StakingType.PARACHAIN
        "aura-relaychain" -> Chain.Asset.StakingType.RELAYCHAIN_AURA
        "turing" -> Chain.Asset.StakingType.TURING
        "aleph-zero" -> Chain.Asset.StakingType.ALEPH_ZERO
        else -> Chain.Asset.StakingType.UNSUPPORTED
    }
}

private fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String = stakingType.name
private fun mapStakingTypeFromLocal(stakingTypeLocal: String): Chain.Asset.StakingType = enumValueOf(stakingTypeLocal)

private fun mapSectionRemoteToSection(sectionRemote: ChainExternalApiRemote.Section?) = sectionRemote?.let {
    Chain.ExternalApi.Section(
        type = mapSectionTypeRemoteToSectionType(sectionRemote.type),
        url = sectionRemote.url
    )
}

private fun mapSectionLocalToSection(sectionLocal: ChainLocal.ExternalApi.Section?) = sectionLocal?.let {
    Chain.ExternalApi.Section(
        type = mapSectionTypeLocalToSectionType(sectionLocal.type),
        url = sectionLocal.url
    )
}

private fun mapSectionToSectionLocal(sectionLocal: Chain.ExternalApi.Section?) = sectionLocal?.let {
    ChainLocal.ExternalApi.Section(
        type = mapSectionTypeToSectionTypeLocal(sectionLocal.type),
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

private const val CHAIN_ADDITIONAL_TIP = "defaultTip"

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

private fun mapChainAssetTypeToRaw(type: Chain.Asset.Type): Pair<String, Map<String, Any?>?> = when (type) {
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

fun mapChainRemoteToChain(
    chainRemote: ChainRemote,
): Chain {
    val nodes = chainRemote.nodes.mapIndexed { index, node ->
        Chain.Node(
            url = node.url,
            name = node.name,
            chainId = chainRemote.chainId,
            orderId = index
        )
    }

    val assets = chainRemote.assets.map {
        Chain.Asset(
            iconUrl = it.icon,
            chainId = chainRemote.chainId,
            id = it.assetId,
            symbol = it.symbol,
            precision = it.precision,
            name = it.name ?: chainRemote.name,
            priceId = it.priceId,
            staking = mapStakingStringToStakingType(it.staking),
            type = mapChainAssetTypeFromRaw(it.type, it.typeExtras),
            buyProviders = it.buyProviders.orEmpty()
        )
    }

    val explorers = chainRemote.explorers.orEmpty().map {
        Chain.Explorer(
            name = it.name,
            account = it.account,
            extrinsic = it.extrinsic,
            event = it.event,
            chainId = chainRemote.chainId
        )
    }

    val types = chainRemote.types?.let {
        Chain.Types(
            url = it.url,
            overridesCommon = it.overridesCommon
        )
    }

    val externalApi = chainRemote.externalApi?.let { externalApi ->
        Chain.ExternalApi(
            history = mapSectionRemoteToSection(externalApi.history),
            staking = mapSectionRemoteToSection(externalApi.staking),
            crowdloans = mapSectionRemoteToSection(externalApi.crowdloans),
            governance = mapSectionRemoteToSection(externalApi.governance)
        )
    }

    val additional = chainRemote.additional?.let {
        Chain.Additional(
            defaultTip = (it[CHAIN_ADDITIONAL_TIP] as? String)?.toBigInteger()
        )
    }

    return with(chainRemote) {
        val optionsOrEmpty = options.orEmpty()

        Chain(
            id = chainId,
            parentId = parentId,
            name = name,
            assets = assets,
            types = types,
            nodes = nodes,
            explorers = explorers,
            icon = icon,
            externalApi = externalApi,
            addressPrefix = addressPrefix,
            isEthereumBased = ETHEREUM_OPTION in optionsOrEmpty,
            isTestNet = TESTNET_OPTION in optionsOrEmpty,
            hasCrowdloans = CROWDLOAN_OPTION in optionsOrEmpty,
            governance = optionsOrEmpty.governanceTypeFromOptions(),
            additional = additional
        )
    }
}

private fun Set<String>.governanceTypeFromOptions(): Chain.Governance {
    return when {
        "governance" in this -> Chain.Governance.V2 // for backward compatibility of dev builds. Can be removed once everyone will update dev app
        "governance-v2" in this -> Chain.Governance.V2
        "governance-v1" in this -> Chain.Governance.V1
        else -> Chain.Governance.NONE
    }
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
            type = mapChainAssetTypeFromRaw(it.type, typeExtrasParsed)
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

    val externalApi = chainLocal.chain.externalApi?.let { externalApi ->
        Chain.ExternalApi(
            staking = mapSectionLocalToSection(externalApi.staking),
            history = mapSectionLocalToSection(externalApi.history),
            crowdloans = mapSectionLocalToSection(externalApi.crowdloans),
            governance = mapSectionLocalToSection(externalApi.governance)
        )
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
            governance = Chain.Governance.valueOf(governance),
            additional = additional
        )
    }
}

fun mapChainNodeToLocal(node: Chain.Node): ChainNodeLocal {
    return ChainNodeLocal(
        url = node.url,
        name = node.name,
        chainId = node.chainId,
        orderId = node.orderId
    )
}

fun mapChainAssetToLocal(asset: Chain.Asset, gson: Gson): ChainAssetLocal = with(asset) {
    val (type, typeExtras) = mapChainAssetTypeToRaw(type)

    return ChainAssetLocal(
        id = id,
        symbol = symbol,
        precision = precision,
        chainId = chainId,
        name = name,
        priceId = priceId,
        staking = mapStakingTypeToLocal(staking),
        type = type,
        buyProviders = gson.toJson(buyProviders),
        typeExtras = gson.toJson(typeExtras),
        icon = iconUrl
    )
}

fun mapChainExplorersToLocal(explorer: Chain.Explorer): ChainExplorerLocal {
    return with(explorer) {
        ChainExplorerLocal(
            chainId = chainId,
            name = name,
            extrinsic = extrinsic,
            account = account,
            event = event
        )
    }
}

fun mapChainToChainLocal(chain: Chain, gson: Gson): ChainLocal {
    val additional = chain.additional?.let { gson.toJson(it) }

    val types = chain.types?.let {
        ChainLocal.TypesConfig(
            url = it.url,
            overridesCommon = it.overridesCommon
        )
    }

    val externalApi = chain.externalApi?.let { externalApi ->
        ChainLocal.ExternalApi(
            staking = mapSectionToSectionLocal(externalApi.staking),
            history = mapSectionToSectionLocal(externalApi.history),
            crowdloans = mapSectionToSectionLocal(externalApi.crowdloans),
            governance = mapSectionToSectionLocal(externalApi.governance)
        )
    }

    val chainLocal = with(chain) {
        ChainLocal(
            id = id,
            parentId = parentId,
            name = name,
            types = types,
            icon = icon,
            prefix = addressPrefix,
            externalApi = externalApi,
            isEthereumBased = isEthereumBased,
            isTestNet = isTestNet,
            hasCrowdloans = hasCrowdloans,
            governance = governance.name,
            additional = additional,
        )
    }

    return chainLocal
}
