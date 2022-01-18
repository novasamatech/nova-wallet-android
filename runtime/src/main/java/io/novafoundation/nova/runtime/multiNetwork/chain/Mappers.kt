package io.novafoundation.nova.runtime.multiNetwork.chain

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.common.utils.parseArbitraryObject
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainExternalApiRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote

private const val ETHEREUM_OPTION = "ethereumBased"
private const val CROWDLOAN_OPTION = "crowdloans"
private const val TESTNET_OPTION = "testnet"

private fun mapSectionTypeRemoteToSectionType(section: String) = when (section) {
    "subquery" -> Chain.ExternalApi.Section.Type.SUBQUERY
    "github" -> Chain.ExternalApi.Section.Type.GITHUB
    else -> Chain.ExternalApi.Section.Type.UNKNOWN
}

private fun mapSectionTypeToSectionTypeLocal(sectionType: Chain.ExternalApi.Section.Type): String = sectionType.name
private fun mapSectionTypeLocalToSectionType(sectionType: String): Chain.ExternalApi.Section.Type = enumValueOf(sectionType)

private fun mapStakingStringToStakingType(stakingString: String?): Chain.Asset.StakingType {
    return when (stakingString) {
        null -> Chain.Asset.StakingType.UNSUPPORTED
        "relaychain" -> Chain.Asset.StakingType.RELAYCHAIN
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
private const val ASSET_UNSUPPORTED = "unsupported"

private const val STATEMINE_EXTRAS_ID = "assetId"

private fun mapChainAssetTypeFromRaw(type: String?, typeExtras: Map<String, Any?>?): Chain.Asset.Type = when (type) {
    null, ASSET_NATIVE -> Chain.Asset.Type.Native
    ASSET_STATEMINE -> {
        val id = typeExtras?.get(STATEMINE_EXTRAS_ID)?.asGsonParsedNumber()

        if (id != null) {
            Chain.Asset.Type.Statemine(id)
        } else {
            Chain.Asset.Type.Unsupported
        }
    }
    else -> Chain.Asset.Type.Unsupported
}

private fun mapChainAssetTypeToRaw(type: Chain.Asset.Type): Pair<String, Map<String, Any?>?> = when (type) {
    is Chain.Asset.Type.Native -> ASSET_NATIVE to null
    is Chain.Asset.Type.Statemine -> ASSET_STATEMINE to mapOf(
        STATEMINE_EXTRAS_ID to type.id
    )
    Chain.Asset.Type.Unsupported -> ASSET_UNSUPPORTED to null
}

fun mapChainRemoteToChain(
    chainRemote: ChainRemote,
): Chain {
    val nodes = chainRemote.nodes.map {
        Chain.Node(
            url = it.url,
            name = it.name
        )
    }

    val assets = chainRemote.assets.map {
        Chain.Asset(
            iconUrl = it.icon ?: chainRemote.icon,
            chainId = chainRemote.chainId,
            id = it.assetId,
            symbol = it.symbol,
            precision = it.precision,
            name = it.name ?: chainRemote.name,
            priceId = it.priceId,
            staking = mapStakingStringToStakingType(it.staking),
            type = mapChainAssetTypeFromRaw(it.type, it.typeExtras)
        )
    }

    val explorers = chainRemote.explorers.orEmpty().map {
        Chain.Explorer(
            name = it.name,
            account = it.account,
            extrinsic = it.extrinsic,
            event = it.event
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
            hasCrowdloans = CROWDLOAN_OPTION in optionsOrEmpty
        )
    }
}

fun mapChainLocalToChain(chainLocal: JoinedChainInfo, gson: Gson): Chain {
    val nodes = chainLocal.nodes.map {
        Chain.Node(
            url = it.url,
            name = it.name
        )
    }

    val assets = chainLocal.assets.map {
        val typeExtrasParsed = it.typeExtras?.let(gson::parseArbitraryObject)

        Chain.Asset(
            iconUrl = it.icon ?: chainLocal.chain.icon,
            id = it.id,
            symbol = it.symbol,
            precision = it.precision,
            name = it.name,
            chainId = it.chainId,
            priceId = it.priceId,
            staking = mapStakingTypeFromLocal(it.staking),
            type = mapChainAssetTypeFromRaw(it.type, typeExtrasParsed)
        )
    }

    val explorers = chainLocal.explorers.map {
        Chain.Explorer(
            name = it.name,
            account = it.account,
            extrinsic = it.extrinsic,
            event = it.event
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
            crowdloans = mapSectionLocalToSection(externalApi.crowdloans)
        )
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
            hasCrowdloans = hasCrowdloans
        )
    }
}

fun mapChainToChainLocal(chain: Chain, gson: Gson): JoinedChainInfo {
    val nodes = chain.nodes.map {
        ChainNodeLocal(
            url = it.url,
            name = it.name,
            chainId = chain.id
        )
    }

    val assets = chain.assets.map {
        val (type, typeExtras) = mapChainAssetTypeToRaw(it.type)

        ChainAssetLocal(
            id = it.id,
            symbol = it.symbol,
            precision = it.precision,
            chainId = chain.id,
            name = it.name,
            priceId = it.priceId,
            staking = mapStakingTypeToLocal(it.staking),
            type = type,
            typeExtras = gson.toJson(typeExtras),
            icon = it.iconUrl
        )
    }

    val explorers = chain.explorers.map {
        ChainExplorerLocal(
            chainId = chain.id,
            name = it.name,
            extrinsic = it.extrinsic,
            account = it.account,
            event = it.event
        )
    }

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
            crowdloans = mapSectionToSectionLocal(externalApi.crowdloans)
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
            hasCrowdloans = hasCrowdloans
        )
    }

    return JoinedChainInfo(
        chain = chainLocal,
        nodes = nodes,
        assets = assets,
        explorers = explorers
    )
}
