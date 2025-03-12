package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.asGsonParsedIntOrNull
import io.novafoundation.nova.common.utils.asGsonParsedLongOrNull
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.ApiType
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal.SourceType
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.Companion.EMPTY_CHAIN_ICON
import io.novafoundation.nova.core_db.model.chain.ChainLocal.ConnectionStateLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal.AutoBalanceStrategyLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote

private const val ETHEREUM_OPTION = "ethereumBased"
private const val CROWDLOAN_OPTION = "crowdloans"
private const val TESTNET_OPTION = "testnet"
private const val PROXY_OPTION = "proxy"
private const val SWAP_HUB = "swap-hub"
private const val HYDRA_DX_SWAPS = "hydradx-swaps"
private const val NO_SUBSTRATE_RUNTIME = "noSubstrateRuntime"
private const val FULL_SYNC_BY_DEFAULT = "fullSyncByDefault"
private const val PUSH_SUPPORT = "pushSupport"
private const val CUSTOM_FEE_ASSET_HUB = "assethub-fees"
private const val CUSTOM_FEE_HYDRA_DX = "hydration-fees"

private const val CHAIN_ADDITIONAL_TIP = "defaultTip"
private const val CHAIN_THEME_COLOR = "themeColor"
private const val CHAIN_STAKING_WIKI = "stakingWiki"
private const val DEFAULT_BLOCK_TIME = "defaultBlockTime"
private const val RELAYCHAIN_AS_NATIVE = "relaychainAsNative"
private const val MAX_ELECTING_VOTES = "stakingMaxElectingVoters"
private const val FEE_VIA_RUNTIME_CALL = "feeViaRuntimeCall"
private const val SUPPORT_GENERIC_LEDGER_APP = "supportsGenericLedgerApp"
private const val IDENTITY_CHAIN = "identityChain"
private const val DISABLED_CHECK_METADATA_HASH = "disabledCheckMetadataHash"
private const val SESSION_LENGTH = "sessionLength"

fun mapRemoteChainToLocal(
    chainRemote: ChainRemote,
    oldChain: ChainLocal?,
    source: ChainLocal.Source,
    gson: Gson
): ChainLocal {
    val types = chainRemote.types?.let {
        ChainLocal.TypesConfig(
            url = it.url.orEmpty(),
            overridesCommon = it.overridesCommon
        )
    }

    val additional = chainRemote.additional?.let {
        Chain.Additional(
            defaultTip = (it[CHAIN_ADDITIONAL_TIP] as? String)?.toBigInteger(),
            themeColor = (it[CHAIN_THEME_COLOR] as? String),
            stakingWiki = (it[CHAIN_STAKING_WIKI] as? String),
            defaultBlockTimeMillis = it[DEFAULT_BLOCK_TIME].asGsonParsedLongOrNull(),
            relaychainAsNative = it[RELAYCHAIN_AS_NATIVE] as? Boolean,
            stakingMaxElectingVoters = it[MAX_ELECTING_VOTES].asGsonParsedIntOrNull(),
            feeViaRuntimeCall = it[FEE_VIA_RUNTIME_CALL] as? Boolean,
            supportLedgerGenericApp = it[SUPPORT_GENERIC_LEDGER_APP] as? Boolean,
            identityChain = it[IDENTITY_CHAIN] as? ChainId,
            disabledCheckMetadataHash = it[DISABLED_CHECK_METADATA_HASH] as? Boolean,
            sessionLength = it[SESSION_LENGTH].asGsonParsedIntOrNull()
        )
    }

    val chainLocal = with(chainRemote) {
        val optionsOrEmpty = options.orEmpty()

        ChainLocal(
            id = chainId,
            parentId = parentId,
            name = name,
            types = types,
            icon = icon ?: EMPTY_CHAIN_ICON,
            prefix = addressPrefix,
            legacyPrefix = legacyAddressPrefix,
            isEthereumBased = ETHEREUM_OPTION in optionsOrEmpty,
            isTestNet = TESTNET_OPTION in optionsOrEmpty,
            hasCrowdloans = CROWDLOAN_OPTION in optionsOrEmpty,
            supportProxy = PROXY_OPTION in optionsOrEmpty,
            hasSubstrateRuntime = NO_SUBSTRATE_RUNTIME !in optionsOrEmpty,
            pushSupport = PUSH_SUPPORT in optionsOrEmpty,
            governance = mapGovernanceRemoteOptionsToLocal(optionsOrEmpty),
            swap = mapSwapRemoteOptionsToLocal(optionsOrEmpty),
            customFee = mapCustomFeeRemoteOptionsToLocal(optionsOrEmpty),
            connectionState = determineConnectionState(chainRemote, oldChain),
            additional = gson.toJson(additional),
            nodeSelectionStrategy = mapNodeSelectionStrategyToLocal(nodeSelectionStrategy),
            source = source
        )
    }

    return chainLocal
}

private fun mapNodeSelectionStrategyToLocal(remote: String?): AutoBalanceStrategyLocal {
    return when (remote) {
        null, "roundRobin" -> AutoBalanceStrategyLocal.ROUND_ROBIN
        "uniform" -> AutoBalanceStrategyLocal.UNIFORM
        else -> AutoBalanceStrategyLocal.UNKNOWN
    }
}

private fun determineConnectionState(remoteChain: ChainRemote, oldLocalChain: ChainLocal?): ConnectionStateLocal {
    if (oldLocalChain != null && oldLocalChain.connectionState.isNotDefault()) {
        return oldLocalChain.connectionState
    }

    val options = remoteChain.options.orEmpty()
    val fullSyncByDefault = FULL_SYNC_BY_DEFAULT in options
    val hasNoSubstrateRuntime = NO_SUBSTRATE_RUNTIME in options

    return if (fullSyncByDefault || hasNoSubstrateRuntime) ConnectionStateLocal.FULL_SYNC else ConnectionStateLocal.LIGHT_SYNC
}

private fun ConnectionStateLocal.isNotDefault(): Boolean {
    return this != ConnectionStateLocal.LIGHT_SYNC
}

private fun mapGovernanceRemoteOptionsToLocal(remoteOptions: Set<String>): String {
    val domainGovernanceTypes = remoteOptions.governanceTypesFromOptions()

    return mapGovernanceListToLocal(domainGovernanceTypes)
}

private fun mapSwapRemoteOptionsToLocal(remoteOptions: Set<String>): String {
    val domainGovernanceTypes = remoteOptions.swapTypesFromOptions()

    return mapSwapListToLocal(domainGovernanceTypes)
}

private fun mapCustomFeeRemoteOptionsToLocal(remoteOptions: Set<String>): String {
    val domainGovernanceTypes = remoteOptions.customFeeTypeFromOptions()

    return mapCustomFeeToLocal(domainGovernanceTypes)
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
        staking = mapRemoteStakingTypesToLocal(assetRemote.staking),
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
            orderId = index,
            source = ChainNodeLocal.Source.DEFAULT
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

fun mapExternalApisToLocal(chainRemote: ChainRemote): List<ChainExternalApiLocal> {
    return chainRemote.externalApi?.flatMap { (apiType, apis) ->
        apis.map { api ->
            ChainExternalApiLocal(
                chainId = chainRemote.chainId,
                sourceType = mapSourceTypeRemoteToLocal(api.sourceType),
                apiType = mapApiTypeRemoteToLocal(apiType),
                parameters = api.parameters,
                url = api.url
            )
        }
    }.orEmpty()
}

private fun mapApiTypeRemoteToLocal(apiType: String): ApiType = when (apiType) {
    "history" -> ApiType.TRANSFERS
    "staking" -> ApiType.STAKING
    "crowdloans" -> ApiType.CROWDLOANS
    "governance" -> ApiType.GOVERNANCE_REFERENDA
    "governance-delegations" -> ApiType.GOVERNANCE_DELEGATIONS
    "referendum-summary" -> ApiType.REFERENDUM_SUMMARY
    else -> ApiType.UNKNOWN
}

private fun mapSourceTypeRemoteToLocal(sourceType: String): SourceType = when (sourceType) {
    "subquery" -> SourceType.SUBQUERY
    "github" -> SourceType.GITHUB
    "polkassembly" -> SourceType.POLKASSEMBLY
    "etherscan" -> SourceType.ETHERSCAN
    "subsquare" -> SourceType.SUBSQUARE
    else -> SourceType.UNKNOWN
}

private fun mapRemoteStakingTypesToLocal(stakingTypesRemote: List<String>?): String {
    return stakingTypesRemote.orEmpty().joinToString(separator = ",") { stakingTypeRemote ->
        val stakingType = mapStakingStringToStakingType(stakingTypeRemote)
        mapStakingTypeToLocal(stakingType)
    }
}

fun mapStakingStringToStakingType(stakingString: String?): Chain.Asset.StakingType {
    return when (stakingString) {
        null -> Chain.Asset.StakingType.UNSUPPORTED
        "relaychain" -> Chain.Asset.StakingType.RELAYCHAIN
        "parachain" -> Chain.Asset.StakingType.PARACHAIN
        "nomination-pools" -> Chain.Asset.StakingType.NOMINATION_POOLS
        "aura-relaychain" -> Chain.Asset.StakingType.RELAYCHAIN_AURA
        "turing" -> Chain.Asset.StakingType.TURING
        "aleph-zero" -> Chain.Asset.StakingType.ALEPH_ZERO
        "mythos" -> Chain.Asset.StakingType.MYTHOS
        else -> Chain.Asset.StakingType.UNSUPPORTED
    }
}

fun mapStakingTypeToStakingString(stakingType: Chain.Asset.StakingType): String? {
    return when (stakingType) {
        Chain.Asset.StakingType.UNSUPPORTED -> null
        Chain.Asset.StakingType.RELAYCHAIN -> "relaychain"
        Chain.Asset.StakingType.PARACHAIN -> "parachain"
        Chain.Asset.StakingType.RELAYCHAIN_AURA -> "aura-relaychain"
        Chain.Asset.StakingType.TURING -> "turing"
        Chain.Asset.StakingType.ALEPH_ZERO -> "aleph-zero"
        Chain.Asset.StakingType.NOMINATION_POOLS -> "nomination-pools"
        Chain.Asset.StakingType.MYTHOS -> "mythos"
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

private fun Set<String>.swapTypesFromOptions(): List<Chain.Swap> {
    return mapNotNull { option ->
        when (option) {
            SWAP_HUB -> Chain.Swap.ASSET_CONVERSION
            HYDRA_DX_SWAPS -> Chain.Swap.HYDRA_DX
            else -> null
        }
    }
}

private fun Set<String>.customFeeTypeFromOptions(): List<Chain.CustomFee> {
    return mapNotNull { option ->
        when (option) {
            CUSTOM_FEE_ASSET_HUB -> Chain.CustomFee.ASSET_HUB
            CUSTOM_FEE_HYDRA_DX -> Chain.CustomFee.HYDRA_DX
            else -> null
        }
    }
}
