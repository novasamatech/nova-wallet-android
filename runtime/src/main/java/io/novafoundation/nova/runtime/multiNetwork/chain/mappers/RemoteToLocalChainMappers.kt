package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainTransferHistoryApiLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainExternalApiRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote

private const val ETHEREUM_OPTION = "ethereumBased"
private const val CROWDLOAN_OPTION = "crowdloans"
private const val TESTNET_OPTION = "testnet"
private const val CHAIN_ADDITIONAL_TIP = "defaultTip"

fun mapSectionTypeToSectionTypeLocal(sectionType: Chain.ExternalApi.Section.Type): String = sectionType.name

fun mapRemoteChainToLocal(
    chainRemote: ChainRemote,
    gson: Gson
): ChainLocal {
    val types = chainRemote.types?.let {
        ChainLocal.TypesConfig(
            url = it.url,
            overridesCommon = it.overridesCommon
        )
    }

    val externalApi = chainRemote.externalApi?.let { externalApi ->
        ChainLocal.ExternalApi(
            staking = mapSectionRemoteToLocal(externalApi.staking),
            crowdloans = mapSectionRemoteToLocal(externalApi.crowdloans),
            governance = mapGovernanceSectionRemoteToLocal(externalApi.governance, gson)
        )
    }

    val additional = chainRemote.additional?.let {
        Chain.Additional(
            defaultTip = (it[CHAIN_ADDITIONAL_TIP] as? String)?.toBigInteger()
        )
    }

    val chainLocal = with(chainRemote) {
        val optionsOrEmpty = options.orEmpty()

        ChainLocal(
            id = chainId,
            parentId = parentId,
            name = name,
            types = types,
            icon = icon,
            prefix = addressPrefix,
            externalApi = externalApi,
            isEthereumBased = ETHEREUM_OPTION in optionsOrEmpty,
            isTestNet = TESTNET_OPTION in optionsOrEmpty,
            hasCrowdloans = CROWDLOAN_OPTION in optionsOrEmpty,
            governance = mapGovernanceRemoteOptionsToLocal(optionsOrEmpty),
            additional = gson.toJson(additional),
        )
    }

    return chainLocal
}

fun mapGovernanceListToLocal(governance: List<Chain.Governance>) = governance.joinToString(separator = ",", transform = Chain.Governance::name)

fun mapGovernanceRemoteOptionsToLocal(remoteOptions: Set<String>): String {
    val domainGovernanceTypes = remoteOptions.governanceTypesFromOptions()

    return mapGovernanceListToLocal(domainGovernanceTypes)
}

fun mapRemoteAssetToLocal(
    chainRemote: ChainRemote,
    assetRemote: ChainAssetRemote,
    gson: Gson,
    isEnabled: Boolean
): ChainAssetLocal {
    return ChainAssetLocal(
        id = assetRemote.assetId,
        symbol = assetRemote.symbol,
        precision = assetRemote.precision,
        chainId = chainRemote.chainId,
        name = assetRemote.name ?: chainRemote.name,
        priceId = assetRemote.priceId,
        staking = mapRemoteStakingTypeToLocal(assetRemote.staking),
        type = assetRemote.type,
        source = AssetSourceLocal.DEFAULT,
        buyProviders = gson.toJson(assetRemote.buyProviders),
        typeExtras = gson.toJson(assetRemote.typeExtras),
        icon = assetRemote.icon,
        enabled = isEnabled
    )
}

fun mapRemoteNodesToLocal(chainRemote: ChainRemote): List<ChainNodeLocal> {
    return chainRemote.nodes.mapIndexed { index, chainNodeRemote ->
        ChainNodeLocal(
            url = chainNodeRemote.url,
            name = chainNodeRemote.name,
            chainId = chainRemote.chainId,
            orderId = index
        )
    }
}

fun mapRemoteExplorersToLocal(chainRemote: ChainRemote): List<ChainExplorerLocal> {
    val explorers = chainRemote.explorers?.map {
        ChainExplorerLocal(
            chainId = chainRemote.chainId,
            name = it.name,
            extrinsic = it.extrinsic,
            account = it.account,
            event = it.event
        )
    }

    return explorers.orEmpty()
}

fun mapRemoteTransferApisToLocal(chainRemote: ChainRemote): List<ChainTransferHistoryApiLocal> {
    val explorers = chainRemote.externalApi?.history.orEmpty().map {
        ChainTransferHistoryApiLocal(
            chainId = chainRemote.chainId,
            assetType = mapTransferApiAssetTypeToLocal(it.assetType),
            apiType = mapTransferHistoryApiTypeRemoteToLocal(it.type),
            url = it.url
        )
    }

    return explorers
}

private fun mapTransferApiAssetTypeToLocal(type: String?): ChainTransferHistoryApiLocal.AssetType {
    return when (type) {
        null, "substrate" -> ChainTransferHistoryApiLocal.AssetType.SUBSTRATE
        "evm" -> ChainTransferHistoryApiLocal.AssetType.EVM
        else -> ChainTransferHistoryApiLocal.AssetType.UNSUPPORTED
    }
}

private fun mapSectionRemoteToLocal(sectionRemote: ChainExternalApiRemote.Section?) = sectionRemote?.let {
    ChainLocal.ExternalApi.Section(
        type = mapSectionTypeRemoteToLocal(sectionRemote.type),
        url = sectionRemote.url
    )
}

private fun mapGovernanceSectionRemoteToLocal(sectionRemote: ChainExternalApiRemote.GovernanceSection?, gson: Gson) = sectionRemote?.let {
    ChainLocal.ExternalApi.GovernanceSection(
        type = mapSectionTypeRemoteToLocal(sectionRemote.type),
        url = sectionRemote.url,
        parameters = gson.toJson(sectionRemote.parameters)
    )
}

private fun mapSectionTypeRemoteToLocal(section: String): String {
    val sectionType = mapSectionTypeRemoteToSectionType(section)
    return mapSectionTypeToSectionTypeLocal(sectionType)
}

private fun mapTransferHistoryApiTypeRemoteToLocal(type: String): ChainTransferHistoryApiLocal.ApiType {
    val sectionType = mapSectionTypeRemoteToSectionType(type)
    return mapApiSectionTypeTypeToTransferHistoryApiLocal(sectionType)
}

private fun mapApiSectionTypeTypeToTransferHistoryApiLocal(type: Chain.ExternalApi.Section.Type): ChainTransferHistoryApiLocal.ApiType {
    return when (type) {
        Chain.ExternalApi.Section.Type.SUBQUERY -> ChainTransferHistoryApiLocal.ApiType.SUBQUERY
        Chain.ExternalApi.Section.Type.GITHUB -> ChainTransferHistoryApiLocal.ApiType.GITHUB
        Chain.ExternalApi.Section.Type.UNKNOWN -> ChainTransferHistoryApiLocal.ApiType.UNKNOWN
        Chain.ExternalApi.Section.Type.POLKASSEMBLY -> ChainTransferHistoryApiLocal.ApiType.POLKASSEMBLY
        Chain.ExternalApi.Section.Type.ETHERSCAN -> ChainTransferHistoryApiLocal.ApiType.ETHERSCAN
    }
}

private fun mapSectionTypeRemoteToSectionType(section: String) = when (section) {
    "subquery" -> Chain.ExternalApi.Section.Type.SUBQUERY
    "github" -> Chain.ExternalApi.Section.Type.GITHUB
    "polkassembly" -> Chain.ExternalApi.Section.Type.POLKASSEMBLY
    "etherscan" -> Chain.ExternalApi.Section.Type.ETHERSCAN
    else -> Chain.ExternalApi.Section.Type.UNKNOWN
}

private fun mapRemoteStakingTypeToLocal(stakingString: String?): String {
    val stakingType = mapStakingStringToStakingType(stakingString)
    return mapStakingTypeToLocal(stakingType)
}

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

private fun Set<String>.governanceTypesFromOptions(): List<Chain.Governance> {
    return mapNotNull { option ->
        when (option) {
            "governance" -> Chain.Governance.V2 // for backward compatibility of dev builds. Can be removed once everyone will update dev app
            "governance-v2" -> Chain.Governance.V2
            "governance-v1" -> Chain.Governance.V1
            else -> null
        }
    }
}
