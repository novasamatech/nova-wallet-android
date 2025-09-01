package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.data.network.runtime.binding.bindOrNull
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.asTokenSymbol
import io.novafoundation.nova.common.utils.emptyEthereumAccountId
import io.novafoundation.nova.common.utils.emptySubstrateAccountId
import io.novafoundation.nova.common.utils.findIsInstanceOrNull
import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.substrateAccountId
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.ALEPH_ZERO
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.MYTHOS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.NOMINATION_POOLS
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.PARACHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.RELAYCHAIN_AURA
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.TURING
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.StakingType.UNSUPPORTED
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ExplorerTemplateExtractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.StatemineAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.TypesUsage
import io.novafoundation.nova.runtime.multiNetwork.chain.model.hasSameId
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.SignatureWrapper
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.extensions.asEthereumAccountId
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.asEthereumPublicKey
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.isValid
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.extensions.toAddress
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHexOrNull
import io.novasama.substrate_sdk_android.runtime.definitions.types.toHexUntyped
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.addressPrefix
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import java.math.BigInteger

const val EVM_DEFAULT_TOKEN_DECIMALS = 18

private const val EIP_155_PREFIX = "eip155"

val Chain.autoBalanceEnabled: Boolean
    get() = nodes.wssNodeSelectionStrategy is Chain.Nodes.NodeSelectionStrategy.AutoBalance

val Chain.selectedUnformattedWssNodeUrlOrNull: String?
    get() = if (nodes.wssNodeSelectionStrategy is Chain.Nodes.NodeSelectionStrategy.SelectedNode) {
        nodes.wssNodeSelectionStrategy.unformattedNodeUrl
    } else {
        null
    }

val Chain.isCustomNetwork: Boolean
    get() = source == Chain.Source.CUSTOM

val Chain.typesUsage: TypesUsage
    get() = when {
        types == null -> TypesUsage.NONE
        !types.overridesCommon && types.url != null -> TypesUsage.BOTH
        !types.overridesCommon && types.url == null -> TypesUsage.BASE
        else -> TypesUsage.OWN
    }

val TypesUsage.requiresBaseTypes: Boolean
    get() = this == TypesUsage.BASE || this == TypesUsage.BOTH

val Chain.utilityAsset
    get() = assets.first(Chain.Asset::isUtilityAsset)

val Chain.isSubstrateBased
    get() = !isEthereumBased

val Chain.commissionAsset
    get() = utilityAsset

val Chain.isEnabled
    get() = connectionState != Chain.ConnectionState.DISABLED

val Chain.isDisabled
    get() = !isEnabled

fun Chain.Asset.supportedStakingOptions(): List<Chain.Asset.StakingType> {
    if (staking.isEmpty()) return emptyList()

    return staking.filter { it != UNSUPPORTED }
}

fun Chain.networkType(): NetworkType {
    return if (hasSubstrateRuntime) {
        NetworkType.SUBSTRATE
    } else {
        NetworkType.EVM
    }
}

fun Chain.evmChainIdOrNull(): BigInteger? {
    return if (id.startsWith(EIP_155_PREFIX)) {
        id.removePrefix("$EIP_155_PREFIX:")
            .toBigIntegerOrNull()
    } else {
        null
    }
}

fun Chain.isSwapSupported(): Boolean = swap.isNotEmpty()

fun List<Chain.Swap>.assetConversionSupported(): Boolean {
    return Chain.Swap.ASSET_CONVERSION in this
}

fun List<Chain.Swap>.hydraDxSupported(): Boolean {
    return Chain.Swap.HYDRA_DX in this
}

val Chain.ConnectionState.isFullSync: Boolean
    get() = this == Chain.ConnectionState.FULL_SYNC

val Chain.ConnectionState.isDisabled: Boolean
    get() = this == Chain.ConnectionState.DISABLED

val Chain.ConnectionState.level: Int
    get() = when (this) {
        Chain.ConnectionState.FULL_SYNC -> 2
        Chain.ConnectionState.LIGHT_SYNC -> 1
        Chain.ConnectionState.DISABLED -> 0
    }

fun Chain.Additional?.relaychainAsNative(): Boolean {
    return this?.relaychainAsNative ?: false
}

fun Chain.Additional?.feeViaRuntimeCall(): Boolean {
    return this?.feeViaRuntimeCall ?: false
}

fun Chain.Additional?.isGenericLedgerAppSupported(): Boolean {
    return this?.supportLedgerGenericApp ?: false
}

fun Chain.Additional?.shouldDisableMetadataHashCheck(): Boolean {
    return this?.disabledCheckMetadataHash ?: false
}

fun ChainId.chainIdHexPrefix16(): String {
    return removeHexPrefix()
        .take(32)
        .requireHexPrefix()
}

enum class StakingTypeGroup {

    RELAYCHAIN, PARACHAIN, NOMINATION_POOL, MYTHOS, UNSUPPORTED
}

fun Chain.Asset.StakingType.group(): StakingTypeGroup {
    return when (this) {
        UNSUPPORTED -> StakingTypeGroup.UNSUPPORTED
        RELAYCHAIN, RELAYCHAIN_AURA, ALEPH_ZERO -> StakingTypeGroup.RELAYCHAIN
        PARACHAIN, TURING -> StakingTypeGroup.PARACHAIN
        MYTHOS -> StakingTypeGroup.MYTHOS
        NOMINATION_POOLS -> StakingTypeGroup.NOMINATION_POOL
    }
}

fun Chain.Asset.StakingType.isDirectStaking(): Boolean {
    return when (group()) {
        StakingTypeGroup.RELAYCHAIN, StakingTypeGroup.PARACHAIN -> true
        else -> false
    }
}

fun Chain.Asset.StakingType.isPoolStaking(): Boolean {
    return group() == StakingTypeGroup.NOMINATION_POOL
}

inline fun <reified T : Chain.ExternalApi> Chain.externalApi(): T? {
    return externalApis.findIsInstanceOrNull<T>()
}

inline fun <reified T : Chain.ExternalApi> Chain.hasExternalApi(): Boolean {
    return externalApis.any { it is T }
}

const val UTILITY_ASSET_ID = 0

val Chain.Asset.isUtilityAsset: Boolean
    get() = id == UTILITY_ASSET_ID

inline val Chain.Asset.isCommissionAsset: Boolean
    get() = isUtilityAsset

inline val FullChainAssetId.isUtility: Boolean
    get() = assetId == UTILITY_ASSET_ID

private const val XC_PREFIX = "xc"

fun Chain.Asset.normalizeSymbol(): String {
    return normalizeTokenSymbol(this.symbol.value)
}

fun TokenSymbol.normalize(): TokenSymbol {
    return normalizeTokenSymbol(value).asTokenSymbol()
}

fun normalizeTokenSymbol(symbol: String): String {
    return symbol.removePrefix(XC_PREFIX)
}

val Chain.Node.isWss: Boolean
    get() = connectionType == Chain.Node.ConnectionType.WSS

val Chain.Node.isHttps: Boolean
    get() = connectionType == Chain.Node.ConnectionType.HTTPS

fun Chain.Nodes.wssNodes(): List<Chain.Node> {
    return nodes.filter { it.isWss }
}

fun Chain.Nodes.httpNodes(): List<Chain.Node> {
    return nodes.filter { it.isHttps }
}

fun Chain.Nodes.hasHttpNodes(): Boolean {
    return nodes.any { it.isHttps }
}

val Chain.Asset.disabled: Boolean
    get() = !enabled

val Chain.genesisHash: String?
    get() = id.takeIf {
        runCatching { it.fromHex() }.isSuccess
    }

fun Chain.hasOnlyOneAddressFormat() = legacyAddressPrefix == null

fun Chain.supportsLegacyAddressFormat() = legacyAddressPrefix != null

fun Chain.requireGenesisHash() = requireNotNull(genesisHash)

fun Chain.addressOf(accountId: ByteArray): String {
    return if (isEthereumBased) {
        accountId.toEthereumAddress()
    } else {
        accountId.toAddress(addressPrefix.toShort())
    }
}

fun Chain.addressOf(accountId: AccountIdKey): String {
    return addressOf(accountId.value)
}

fun Chain.legacyAddressOfOrNull(accountId: ByteArray): String? {
    return if (isEthereumBased) {
        null
    } else {
        legacyAddressPrefix?.let { accountId.toAddress(it.toShort()) }
    }
}

fun ByteArray.toEthereumAddress(): String {
    return asEthereumAccountId().toAddress(withChecksum = true).value
}

fun Chain.accountIdOf(address: String): ByteArray {
    return if (isEthereumBased) {
        address.asEthereumAddress().toAccountId().value
    } else {
        address.toAccountId()
    }
}

fun String.toAccountId(chain: Chain): ByteArray {
    return chain.accountIdOf(this)
}

fun String.toAccountIdKey(chain: Chain): AccountIdKey {
    return chain.accountIdKeyOf(this)
}

fun Chain.accountIdKeyOf(address: String): AccountIdKey {
    return accountIdOf(address).intoKey()
}

fun String.anyAddressToAccountId(): ByteArray {
    return runCatching {
        // Substrate
        toAccountId()
    }.recoverCatching {
        // Evm
        asEthereumAddress().toAccountId().value
    }.getOrThrow()
}

fun Chain.accountIdOrNull(address: String): ByteArray? {
    return runCatching { accountIdOf(address) }.getOrNull()
}

fun Chain.emptyAccountId() = if (isEthereumBased) {
    emptyEthereumAccountId()
} else {
    emptySubstrateAccountId()
}

fun Chain.emptyAccountIdKey() = emptyAccountId().intoKey()

fun Chain.accountIdOrDefault(maybeAddress: String): ByteArray {
    return accountIdOrNull(maybeAddress) ?: emptyAccountId()
}

fun Chain.accountIdOf(publicKey: ByteArray): ByteArray {
    return if (isEthereumBased) {
        publicKey.asEthereumPublicKey().toAccountId().value
    } else {
        publicKey.substrateAccountId()
    }
}

fun Chain.hexAccountIdOf(address: String): String {
    return accountIdOf(address).toHexString()
}

fun Chain.multiAddressOf(accountId: ByteArray): MultiAddress {
    return if (isEthereumBased) {
        MultiAddress.Address20(accountId)
    } else {
        MultiAddress.Id(accountId)
    }
}

fun Chain.isValidAddress(address: String): Boolean {
    return runCatching {
        if (isEthereumBased) {
            address.asEthereumAddress().isValid()
        } else {
            address.toAccountId() // verify supplied address can be converted to account id

            addressPrefix.toShort() == address.addressPrefix() ||
                legacyAddressPrefix?.toShort() == address.addressPrefix()
        }
    }.getOrDefault(false)
}

fun Chain.isValidEvmAddress(address: String): Boolean {
    return runCatching {
        if (isEthereumBased) {
            address.asEthereumAddress().isValid()
        } else {
            false
        }
    }.getOrDefault(false)
}

val Chain.isParachain
    get() = parentId != null

fun Chain.multiAddressOf(address: String): MultiAddress = multiAddressOf(accountIdOf(address))

fun Chain.availableExplorersFor(field: ExplorerTemplateExtractor) = explorers.filter { field(it) != null }

fun Chain.Explorer.accountUrlOf(address: String): String {
    return format(Chain.Explorer::account, "address", address)
}

fun Chain.Explorer.extrinsicUrlOf(extrinsicHash: String): String {
    return format(Chain.Explorer::extrinsic, "hash", extrinsicHash)
}

fun Chain.Explorer.eventUrlOf(eventId: String): String {
    return format(Chain.Explorer::event, "event", eventId)
}

private inline fun Chain.Explorer.format(
    templateExtractor: ExplorerTemplateExtractor,
    argumentName: String,
    argumentValue: String,
): String {
    val template = templateExtractor(this) ?: throw Exception("Cannot find template in the chain explorer: $name")

    return template.formatNamed(argumentName to argumentValue)
}

object ChainGeneses {

    const val KUSAMA = "b0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe"
    const val POLKADOT = "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3"
    const val WESTEND = "e143f23803ac50e8f6f8e62695d1ce9e4e1d68aa36c1cd2cfd15340213f3423e"

    const val KUSAMA_ASSET_HUB = "48239ef607d7928874027a43a67689209727dfb3d3dc5e5b03a39bdc2eda771a"

    const val ACALA = "fc41b9bd8ef8fe53d58c7ea67c794c7ec9a73daf05e6d54b14ff6342c99ba64c"

    const val ROCOCO_ACALA = "a84b46a3e602245284bb9a72c4abd58ee979aa7a5d7f8c4dfdddfaaf0665a4ae"

    const val STATEMINT = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"
    const val EDGEWARE = "742a2ca70c2fda6cee4f8df98d64c4c670a052d9568058982dad9d5a7a135c5b"

    const val KARURA = "baf5aabe40646d11f0ee8abbdc64f4a4b7674925cba08e4a05ff9ebed6e2126b"

    const val NODLE_PARACHAIN = "97da7ede98d7bad4e36b4d734b6055425a3be036da2a332ea5a7037656427a21"

    const val MOONBEAM = "fe58ea77779b7abda7da4ec526d14db9b1e9cd40a217c34892af80a9b332b76d"
    const val MOONRIVER = "401a1f9dca3da46f5c4091016c8a2f26dcea05865116b286f60f668207d1474b"

    const val POLYMESH = "6fbd74e5e1d0a61d52ccfe9d4adaed16dd3a7caa37c6bc4d0c2fa12e8b2f4063"

    const val XX_NETWORK = "50dd5d206917bf10502c68fb4d18a59fc8aa31586f4e8856b493e43544aa82aa"

    const val KILT = "411f057b9107718c9624d6aa4a3f23c1653898297f3d4d529d9bb6511a39dd21"

    const val ASTAR = "9eb76c5184c4ab8679d2d5d819fdf90b9c001403e9e17da2e14b6d8aec4029c6"

    const val ALEPH_ZERO = "70255b4d28de0fc4e1a193d7e175ad1ccef431598211c55538f1018651a0344e"
    const val TERNOA = "6859c81ca95ef624c9dfe4dc6e3381c33e5d6509e35e147092bfbc780f777c4e"

    const val POLIMEC = "7eb9354488318e7549c722669dcbdcdc526f1fef1420e7944667212f3601fdbd"

    const val POLKADEX = "3920bcb4960a1eef5580cd5367ff3f430eef052774f78468852f7b9cb39f8a3c"

    const val CALAMARI = "4ac80c99289841dd946ef92765bf659a307d39189b3ce374a92b5f0415ee17a1"

    const val TURING = "0f62b701fb12d02237a33b84818c11f621653d2b1614c777973babf4652b535d"

    const val ZEITGEIST = "1bf2a2ecb4a868de66ea8610f2ce7c8c43706561b6476031315f6640fe38e060"

    const val WESTMINT = "67f9723393ef76214df0118c34bbbd3dbebc8ed46a10973a8c969d48fe7598c9"

    const val HYDRA_DX = "afdc188f45c71dacbaa0b62e16a91f726c7b8699a9748cdf715459de6b7f366d"

    const val AVAIL_TURING_TESTNET = "d3d2f3a3495dc597434a99d7d449ebad6616db45e4e4f178f31cc6fa14378b70"
    const val AVAIL = "b91746b45e0346cc2f815a520b9c6cb4d5c0902af848db0a80f85932d2e8276a"

    const val VARA = "fe1b4c55fd4d668101126434206571a7838a8b6b93a6d1b95d607e78e6c53763"

    const val POLKADOT_ASSET_HUB = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"

    const val UNIQUE_NETWORK = "84322d9cddbf35088f1e54e9a85c967a41a56a4f43445768125e61af166c7d31"

    const val POLKADOT_PEOPLE = "67fa177a097bfa18f77ea95ab56e9bcdfeb0e5b8a40e46298bb93e16b6fc5008"
    const val KUSAMA_PEOPLE = "c1af4cb4eb3918e5db15086c0cc5ec17fb334f728b7c65dd44bfe1e174ff8b3f"
}

object ChainIds {

    const val ETHEREUM = "$EIP_155_PREFIX:1"

    const val MOONBEAM = ChainGeneses.MOONBEAM
    const val MOONRIVER = ChainGeneses.MOONRIVER
}

val Chain.Companion.Geneses
    get() = ChainGeneses

val Chain.Companion.Ids
    get() = ChainIds

fun Chain.Asset.requireStatemine(): Type.Statemine {
    require(type is Type.Statemine)

    return type
}

fun Chain.findStatemineAssets(): List<Chain.Asset> {
    return assets.filter { it.type is Type.Statemine }
}

fun Chain.Asset.statemineOrNull(): Type.Statemine? {
    return type as? Type.Statemine
}

fun Type.Statemine.palletNameOrDefault(): String {
    return palletName ?: Modules.ASSETS
}

fun Chain.Asset.requireOrml(): Type.Orml {
    require(type is Type.Orml)

    return type
}

val Chain.addressScheme: AddressScheme
    get() = if (isEthereumBased) AddressScheme.EVM else AddressScheme.SUBSTRATE

fun Chain.Asset.ormlOrNull(): Type.Orml? {
    return type as? Type.Orml
}

fun Chain.Asset.requireErc20(): Type.EvmErc20 {
    require(type is Type.EvmErc20)

    return type
}

fun Chain.Asset.requireEquilibrium(): Type.Equilibrium {
    require(type is Type.Equilibrium)

    return type
}

fun Chain.Asset.ormlCurrencyId(runtime: RuntimeSnapshot): Any? {
    return requireOrml().currencyId(runtime)
}

fun Type.Orml.currencyId(runtime: RuntimeSnapshot): Any? {
    val currencyIdType = runtime.typeRegistry[currencyIdType]
        ?: error("Cannot find type $currencyIdType")

    return currencyIdType.fromHex(runtime, currencyIdScale)
}

val Chain.Asset.fullId: FullChainAssetId
    get() = FullChainAssetId(chainId, id)

fun Chain.enabledAssets(): List<Chain.Asset> = assets.filter { it.enabled }

fun Chain.disabledAssets(): List<Chain.Asset> = assets.filterNot { it.enabled }

fun evmChainIdFrom(chainId: Int) = "$EIP_155_PREFIX:$chainId"

fun evmChainIdFrom(chainId: BigInteger) = "$EIP_155_PREFIX:$chainId"

fun Chain.findAssetByOrmlCurrencyId(runtime: RuntimeSnapshot, currencyId: Any?): Chain.Asset? {
    return assets.find { asset ->
        if (asset.type !is Type.Orml) return@find false
        val currencyType = runtime.typeRegistry[asset.type.currencyIdType] ?: return@find false

        val currencyIdScale = bindOrNull { currencyType.toHexUntyped(runtime, currencyId) } ?: return@find false

        currencyIdScale == asset.type.currencyIdScale
    }
}

fun Chain.findAssetByStatemineAssetId(runtime: RuntimeSnapshot, assetId: Any?): Chain.Asset? {
    return assets.find { asset ->
        if (asset.type !is Type.Statemine) return@find false

        asset.type.hasSameId(runtime, assetId)
    }
}

fun Type.Orml.decodeOrNull(runtime: RuntimeSnapshot): Any? {
    val currencyType = runtime.typeRegistry[currencyIdType] ?: return null
    return currencyType.fromHexOrNull(runtime, currencyIdScale)
}

val Chain.Asset.localId: AssetAndChainId
    get() = AssetAndChainId(chainId, id)

val Chain.Asset.onChainAssetId: String?
    get() = when (this.type) {
        is Type.Equilibrium -> this.type.toString()
        is Type.Orml -> this.type.currencyIdScale
        is Type.Statemine -> this.type.id.onChainAssetId()
        is Type.EvmErc20 -> this.type.contractAddress
        is Type.Native -> null
        is Type.EvmNative -> null
        Type.Unsupported -> error("Unsupported assetId type: ${this.type::class.simpleName}")
    }

fun StatemineAssetId.onChainAssetId(): String {
    return when (this) {
        is StatemineAssetId.Number -> value.toString()
        is StatemineAssetId.ScaleEncoded -> scaleHex
    }
}

fun Chain.openGovIfSupported(): Chain.Governance? {
    return Chain.Governance.V2.takeIf { it in governance }
}

fun Chain.Explorer.normalizedUrl(): String? {
    val url = listOfNotNull(extrinsic, account, event).firstOrNull()
    return url?.let { Urls.normalizeUrl(it) }
}

fun Chain.supportTinderGov(): Boolean {
    return hasReferendaSummaryApi()
}

fun Chain.hasReferendaSummaryApi(): Boolean {
    return externalApi<Chain.ExternalApi.ReferendumSummary>() != null
}

fun Chain.summaryApiOrNull(): Chain.ExternalApi.ReferendumSummary? {
    return externalApi<Chain.ExternalApi.ReferendumSummary>()
}

fun Chain.timelineChainId(): ChainId? {
    return additional?.timelineChain
}

fun Chain.timelineChainIdOrSelf(): ChainId {
    return timelineChainId() ?: id
}

fun FullChainAssetId.Companion.utilityAssetOf(chainId: ChainId) = FullChainAssetId(chainId, UTILITY_ASSET_ID)

fun SignatureVerifier.verifyMultiChain(
    chain: Chain,
    signature: SignatureWrapper,
    message: ByteArray,
    publicKey: ByteArray
): Boolean {
    return if (chain.isEthereumBased) {
        verify(signature, Signer.MessageHashing.ETHEREUM, message, publicKey)
    } else {
        verify(signature, Signer.MessageHashing.SUBSTRATE, message, publicKey)
    }
}
