package io.novafoundation.nova.common.utils

import io.emeraldpay.polkaj.scale.ScaleCodecReader
import io.emeraldpay.polkaj.scale.ScaleCodecWriter
import io.novafoundation.nova.common.data.network.runtime.binding.bindNullableNumberConstant
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberConstant
import io.novafoundation.nova.common.data.network.runtime.binding.fromByteArrayOrIncompatible
import io.novafoundation.nova.common.data.network.runtime.binding.fromHexOrIncompatible
import io.novafoundation.nova.core.model.Node
import jp.co.soramitsu.fearless_utils.encrypt.SignatureWrapper
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import jp.co.soramitsu.fearless_utils.encrypt.seed.SeedFactory
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAccountId
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.extensions.toAddress
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.Hasher.blake2b256
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.RuntimeType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.bytesOrNull
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromByteArrayOrNull
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.DefaultSignedExtensions
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericEvent
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignedRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.genesisHash
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.callOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.fullName
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Constant
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Event
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.FunctionArgument
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.MetadataFunction
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.Module
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.moduleOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.splitKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageOrNull
import jp.co.soramitsu.fearless_utils.scale.EncodableStruct
import jp.co.soramitsu.fearless_utils.scale.Schema
import jp.co.soramitsu.fearless_utils.scale.dataType.DataType
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.addressPrefix
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress
import org.web3j.crypto.Sign
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

typealias PalletName = String

val BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH: String
    get() = "//44//60//0/0/0"

val SS58Encoder.DEFAULT_PREFIX: Short
    get() = 42.toShort()

fun BIP32JunctionDecoder.default() = decode(DEFAULT_DERIVATION_PATH)

fun StorageEntry.defaultInHex() = default.toHexString(withPrefix = true)

fun ByteArray.toAddress(networkType: Node.NetworkType) = toAddress(networkType.runtimeConfiguration.addressByte)

fun String.isValidSS58Address() = runCatching { toAccountId() }.isSuccess

fun String.removeHexPrefix() = removePrefix("0x")

fun MetadataFunction.argument(name: String): FunctionArgument = arguments.first { it.name == name }

fun MetadataFunction.argumentType(name: String): RuntimeType<*, *> = requireNotNull(argument(name).type)

fun FunctionArgument.requireActualType() = type?.skipAliases()!!

fun Short.toByteArray(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
    val buffer = ByteBuffer.allocate(2)
    buffer.order(byteOrder)
    buffer.putShort(this)
    return buffer.array()
}

val Short.bigEndianBytes
    get() = toByteArray(ByteOrder.BIG_ENDIAN)

fun ByteArray.toBigEndianShort(): Short = ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).short
fun ByteArray.toBigEndianU16(): UShort = toBigEndianShort().toUShort()

fun ByteArray.toBigEndianU32(): UInt = ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).int.toUInt()

fun <T> DataType<T>.fromHex(hex: String): T {
    val codecReader = ScaleCodecReader(hex.fromHex())

    return read(codecReader)
}

fun <T> DataType<T>.toHex(value: T): String {
    return toByteArray(value).toHexString(withPrefix = true)
}

fun <T> DataType<T>.toByteArray(value: T): ByteArray {
    val stream = ByteArrayOutputStream()
    val writer = ScaleCodecWriter(stream)

    write(writer, value)

    return stream.toByteArray()
}

fun RuntimeType<*, *>.toHexUntypedOrNull(runtime: RuntimeSnapshot, value: Any?) =
    bytesOrNull(runtime, value)?.toHexString(withPrefix = true)

fun RuntimeSnapshot.isParachain() = metadata.hasModule(Modules.PARACHAIN_SYSTEM)

typealias StructBuilderWithContext<S> = S.(EncodableStruct<S>) -> Unit

operator fun <S : Schema<S>> S.invoke(block: StructBuilderWithContext<S>? = null): EncodableStruct<S> {
    val struct = EncodableStruct(this)

    block?.invoke(this, struct)

    return struct
}

fun <S : Schema<S>> EncodableStruct<S>.hash(): String {
    return schema.toByteArray(this).blake2b256().toHexString(withPrefix = true)
}

fun String.extrinsicHash(): String {
    return fromHex().blake2b256().toHexString(withPrefix = true)
}

fun StorageEntry.decodeValue(value: String?, runtimeSnapshot: RuntimeSnapshot) = value?.let {
    val type = type.value ?: throw IllegalStateException("Unknown value type for storage ${this.fullName}")

    type.fromHexOrIncompatible(it, runtimeSnapshot)
}

fun Constant.decodedValue(runtimeSnapshot: RuntimeSnapshot): Any? {
    val type = type ?: throw IllegalStateException("Unknown value type for constant ${this.name}")

    return type.fromByteArrayOrIncompatible(value, runtimeSnapshot)
}

fun String.toHexAccountId(): String = toAccountId().toHexString()

fun Extrinsic.DecodedInstance.tip(): BigInteger? = signature?.signedExtras?.get(DefaultSignedExtensions.CHECK_TX_PAYMENT) as? BigInteger

fun Module.constant(name: String) = constantOrNull(name) ?: throw NoSuchElementException()

fun Module.numberConstant(name: String, runtimeSnapshot: RuntimeSnapshot) = bindNumberConstant(constant(name), runtimeSnapshot)
fun Module.numberConstantOrNull(name: String, runtimeSnapshot: RuntimeSnapshot) = constantOrNull(name)?.let {
    bindNumberConstant(it, runtimeSnapshot)
}

fun Module.optionalNumberConstant(name: String, runtimeSnapshot: RuntimeSnapshot) = bindNullableNumberConstant(constant(name), runtimeSnapshot)

fun Constant.asNumber(runtimeSnapshot: RuntimeSnapshot) = bindNumberConstant(this, runtimeSnapshot)

fun Constant.decoded(runtimeSnapshot: RuntimeSnapshot): Any? {
    return type?.fromByteArrayOrNull(runtimeSnapshot, value)
}

fun Module.constantOrNull(name: String) = constants[name]

fun RuntimeMetadata.staking() = module(Modules.STAKING)

fun RuntimeMetadata.voterListOrNull() = firstExistingModuleOrNull(Modules.VOTER_LIST, Modules.BAG_LIST)
fun RuntimeMetadata.voterListName(): String = requireNotNull(voterListOrNull()).name

fun RuntimeMetadata.system() = module(Modules.SYSTEM)

fun RuntimeMetadata.balances() = module(Modules.BALANCES)

fun RuntimeMetadata.eqBalances() = module(Modules.EQ_BALANCES)

fun RuntimeMetadata.tokens() = module(Modules.TOKENS)

fun RuntimeMetadata.currencies() = module(Modules.CURRENCIES)
fun RuntimeMetadata.currenciesOrNull() = moduleOrNull(Modules.CURRENCIES)
fun RuntimeMetadata.crowdloan() = module(Modules.CROWDLOAN)
fun RuntimeMetadata.uniques() = module(Modules.UNIQUES)

fun RuntimeMetadata.babe() = module(Modules.BABE)
fun RuntimeMetadata.elections() = module(Modules.ELECTIONS)

fun RuntimeMetadata.electionsOrNull() = moduleOrNull(Modules.ELECTIONS)

fun RuntimeMetadata.committeeManagementOrNull() = moduleOrNull("CommitteeManagement")

fun RuntimeMetadata.babeOrNull() = moduleOrNull(Modules.BABE)

fun RuntimeMetadata.timestampOrNull() = moduleOrNull(Modules.TIMESTAMP)
fun RuntimeMetadata.timestamp() = module(Modules.TIMESTAMP)

fun RuntimeMetadata.slots() = module(Modules.SLOTS)

fun RuntimeMetadata.session() = module(Modules.SESSION)

fun RuntimeMetadata.parachainStaking() = module(Modules.PARACHAIN_STAKING)

fun RuntimeMetadata.vesting() = module(Modules.VESTING)

fun RuntimeMetadata.identity() = module(Modules.IDENTITY)

fun RuntimeMetadata.automationTime() = module(Modules.AUTOMATION_TIME)

fun RuntimeMetadata.parachainInfoOrNull() = moduleOrNull(Modules.PARACHAIN_INFO)
fun RuntimeMetadata.parasOrNull() = moduleOrNull(Modules.PARAS)

fun RuntimeMetadata.referenda() = module(Modules.REFERENDA)

fun RuntimeMetadata.convictionVoting() = module(Modules.CONVICTION_VOTING)

fun RuntimeMetadata.democracy() = module(Modules.DEMOCRACY)

fun RuntimeMetadata.scheduler() = module(Modules.SCHEDULER)

fun RuntimeMetadata.treasury() = module(Modules.TREASURY)

fun RuntimeMetadata.electionProviderMultiPhaseOrNull() = moduleOrNull(Modules.ELECTION_PROVIDER_MULTI_PHASE)

fun RuntimeMetadata.preImage() = module(Modules.PREIMAGE)

fun RuntimeMetadata.nominationPools() = module(Modules.NOMINATION_POOLS)

fun RuntimeMetadata.nominationPoolsOrNull() = moduleOrNull(Modules.NOMINATION_POOLS)

fun RuntimeMetadata.assetConversionOrNull() = moduleOrNull(Modules.ASSET_CONVERSION)

fun RuntimeMetadata.omnipoolOrNull() = moduleOrNull(Modules.OMNIPOOL)

fun RuntimeMetadata.omnipool() = module(Modules.OMNIPOOL)

fun RuntimeMetadata.dynamicFeesOrNull() = moduleOrNull(Modules.DYNAMIC_FEES)

fun RuntimeMetadata.dynamicFees() = module(Modules.DYNAMIC_FEES)

fun RuntimeMetadata.assetConversion() = module(Modules.ASSET_CONVERSION)

fun RuntimeMetadata.proxy() = module(Modules.PROXY)

fun RuntimeMetadata.firstExistingModuleName(vararg options: String): String {
    return options.first(::hasModule)
}

fun RuntimeMetadata.firstExistingModuleOrNull(vararg options: String): Module? {
    return options.tryFindNonNull { moduleOrNull(it) }
}

fun Module.firstExistingCallName(vararg options: String): String {
    return options.first(::hasCall)
}

fun RuntimeMetadata.xcmPalletName() = firstExistingModuleName("XcmPallet", "PolkadotXcm")

fun RuntimeMetadata.xTokensName() = firstExistingModuleName("XTokens", "Xtokens")

fun StorageEntry.splitKeyToComponents(runtime: RuntimeSnapshot, key: String): ComponentHolder {
    return ComponentHolder(splitKey(runtime, key))
}

fun String.networkType() = Node.NetworkType.findByAddressByte(addressPrefix())!!

fun RuntimeMetadata.hasModule(name: String) = moduleOrNull(name) != null
fun RuntimeMetadata.hasConstant(module: String, constant: String) = moduleOrNull(module)?.constantOrNull(constant) != null

fun Module.hasCall(name: String) = callOrNull(name) != null

fun Module.hasStorage(storage: String) = storageOrNull(storage) != null

fun SeedFactory.createSeed32(length: Mnemonic.Length, password: String?) = cropSeedTo32Bytes(createSeed(length, password))

fun SeedFactory.deriveSeed32(mnemonicWords: String, password: String?) = cropSeedTo32Bytes(deriveSeed(mnemonicWords, password))

private fun cropSeedTo32Bytes(seedResult: SeedFactory.Result): SeedFactory.Result {
    return SeedFactory.Result(seed = seedResult.seed.copyOfRange(0, 32), seedResult.mnemonic)
}

fun GenericCall.Instance.oneOf(vararg functionCandidates: MetadataFunction?): Boolean {
    return functionCandidates.any { candidate -> candidate != null && function == candidate }
}

fun GenericCall.Instance.instanceOf(functionCandidate: MetadataFunction): Boolean = function == functionCandidate

fun GenericCall.Instance.instanceOf(moduleName: String, callName: String): Boolean = moduleName == module.name && callName == function.name

fun GenericCall.Instance.instanceOf(moduleName: String, vararg callNames: String): Boolean = moduleName == module.name && function.name in callNames

fun GenericEvent.Instance.instanceOf(moduleName: String, eventName: String): Boolean = moduleName == module.name && eventName == event.name

fun GenericEvent.Instance.instanceOf(event: Event): Boolean = event.index == this.event.index

fun structOf(vararg pairs: Pair<String, Any?>) = Struct.Instance(mapOf(*pairs))

fun SignedRaw.toEcdsaSignatureData(): Sign.SignatureData {
    return signatureWrapper.run {
        require(this is SignatureWrapper.Ecdsa)
        Sign.SignatureData(v, r, s)
    }
}

fun SignedRaw.asHexString() = signatureWrapper.asHexString()

fun SignatureWrapper.asHexString() = signature.toHexString(withPrefix = true)

fun String.ethereumAddressToAccountId() = asEthereumAddress().toAccountId().value
fun AccountId.ethereumAccountIdToAddress(withChecksum: Boolean = true) = asEthereumAccountId().toAddress(withChecksum).value

fun emptyEthereumAccountId() = ByteArray(20) { 1 }
fun emptyEthereumAddress() = emptyEthereumAccountId().ethereumAccountIdToAddress(withChecksum = false)

val SignerPayloadExtrinsic.chainId: String
    get() = genesisHash.toHexString()

fun CallRepresentation.toCallInstance(): CallRepresentation.Instance? {
    return (this as? CallRepresentation.Instance)
}

object Modules {
    const val VESTING: String = "Vesting"
    const val STAKING = "Staking"
    const val BALANCES = "Balances"
    const val EQ_BALANCES = "EqBalances"
    const val SYSTEM = "System"
    const val CROWDLOAN = "Crowdloan"
    const val BABE = "Babe"
    const val ELECTIONS = "Elections"
    const val TIMESTAMP = "Timestamp"
    const val SLOTS = "Slots"
    const val SESSION = "Session"

    const val ASSETS = "Assets"
    const val TOKENS = "Tokens"
    const val CURRENCIES = "Currencies"

    const val UNIQUES = "Uniques"

    const val PARACHAIN_STAKING = "ParachainStaking"

    const val PARACHAIN_SYSTEM = "ParachainSystem"

    const val IDENTITY = "Identity"

    const val PARACHAIN_INFO = "ParachainInfo"
    const val PARAS = "Paras"

    const val AUTOMATION_TIME = "AutomationTime"

    const val REFERENDA = "Referenda"
    const val CONVICTION_VOTING = "ConvictionVoting"

    const val SCHEDULER = "Scheduler"

    const val TREASURY = "Treasury"

    const val PREIMAGE = "Preimage"

    const val DEMOCRACY = "Democracy"

    const val VOTER_LIST = "VoterList"
    const val BAG_LIST = "BagsList"

    const val ELECTION_PROVIDER_MULTI_PHASE = "ElectionProviderMultiPhase"

    const val NOMINATION_POOLS = "NominationPools"

    const val ASSET_CONVERSION = "AssetConversion"

    const val TRANSACTION_PAYMENT = "TransactionPayment"
    const val ASSET_TX_PAYMENT = "AssetTxPayment"

    const val UTILITY = "Utility"

    const val PROXY = "Proxy"

    const val AUCTIONS = "auctions"

    const val INDICES = "Indices"
    const val GRANDPA = "Grandpa"
    const val IM_ONLINE = "ImOnline"
    const val BOUNTIES = "Bounties"
    const val CHILD_BOUNTIES = "ChildBounties"
    const val WHITELIST = "Whitelist"
    const val CLAIMS = "Claims"
    const val MULTISIG = "Multisig"
    const val REGISTRAR = "Registrar"
    const val FAST_UNSTAKE = "FastUnstake"

    const val OMNIPOOL = "Omnipool"

    const val DYNAMIC_FEES = "DynamicFees"
}
