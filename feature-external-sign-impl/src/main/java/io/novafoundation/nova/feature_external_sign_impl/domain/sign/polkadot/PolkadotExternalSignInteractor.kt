package io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.endsWith
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.startsWith
import io.novafoundation.nova.common.validation.EmptyValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.fee.types.assetHub.decodeCustomTxPaymentId
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SigningMode
import io.novafoundation.nova.feature_account_api.data.signer.setSignerData
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.failedSigningIfNotCancelled
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.PolkadotSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.maybeSignExtrinsic
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.BaseExternalSignInteractor
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationSystem
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ExternalSignInteractor
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.tryConvertHexToUtf8
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.anyAddressToAccountId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomTransactionExtensions
import io.novafoundation.nova.runtime.extrinsic.extensions.ChargeAssetTxPayment.Companion.chargeAssetTxPayment
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.extrinsic.signer.generateMetadataProofWithSignerRestrictions
import io.novafoundation.nova.runtime.extrinsic.signer.signRaw
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.EraType
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.ChargeTransactionPayment
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckGenesis
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckMortality
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckSpecVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckTxVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHash
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PolkadotSignInteractorFactory(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val metadataShortenerService: MetadataShortenerService,
    private val signBytesChainResolver: SignBytesChainResolver,
    private val signerProvider: SignerProvider,
    private val signingContextFactory: SigningContext.Factory,
) {

    fun create(request: ExternalSignRequest.Polkadot, wallet: ExternalSignWallet) = PolkadotExternalSignInteractor(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        wallet = wallet,
        signerProvider = signerProvider,
        metadataShortenerService = metadataShortenerService,
        signingContextFactory = signingContextFactory,
        signBytesChainResolver = signBytesChainResolver
    )
}

class PolkadotExternalSignInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val request: ExternalSignRequest.Polkadot,
    private val signerProvider: SignerProvider,
    private val metadataShortenerService: MetadataShortenerService,
    private val signingContextFactory: SigningContext.Factory,
    private val signBytesChainResolver: SignBytesChainResolver,
    wallet: ExternalSignWallet,
    accountRepository: AccountRepository
) : BaseExternalSignInteractor(accountRepository, wallet, signerProvider) {

    private val signPayload = request.payload

    private val actualParsedExtrinsic = singleReplaySharedFlow<DAppParsedExtrinsic>()

    override val validationSystem: ConfirmDAppOperationValidationSystem = EmptyValidationSystem()

    override suspend fun createAccountAddressModel(): AddressModel {
        val icon = addressIconGenerator.createAddressIcon(
            accountId = signPayload.accountId(),
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            backgroundColorRes = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )

        return AddressModel(signPayload.address, icon, name = null)
    }

    override suspend fun chainUi(): Result<ChainUi?> {
        return runCatching {
            signPayload.maybeSignExtrinsic()?.let {
                mapChainToUi(it.chain())
            }
        }
    }

    override fun utilityAssetFlow(): Flow<Chain.Asset>? {
        val chainId = signPayload.maybeSignExtrinsic()?.genesisHash ?: return null

        return flow {
            val chain = chainRegistry.getChainOrNull(chainId) ?: return@flow
            emit(chain.utilityAsset)
        }
    }

    override suspend fun performOperation(upToDateFee: Fee?): ExternalSignCommunicator.Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (signPayload) {
                is PolkadotSignPayload.Json -> signExtrinsic(signPayload)
                is PolkadotSignPayload.Raw -> signBytes(signPayload)
            }
        }
            .onFailure { Log.e("PolkadotExternalSignInteractor", "Failed to sign", it) }
            .fold(
                onSuccess = { signedResult ->
                    ExternalSignCommunicator.Response.Signed(request.id, signedResult.signature, signedResult.modifiedTransaction)
                },
                onFailure = { error ->
                    error.failedSigningIfNotCancelled(request.id)
                }
            )
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (signPayload) {
            is PolkadotSignPayload.Json -> readableExtrinsicContent()
            is PolkadotSignPayload.Raw -> readableBytesContent(signPayload)
        }
    }

    override suspend fun calculateFee(): Fee? = withContext(Dispatchers.Default) {
        require(signPayload is PolkadotSignPayload.Json)

        val chain = signPayload.chainOrNull() ?: return@withContext null

        val signer = resolveWalletSigner()
        val (extrinsic, _, parsedExtrinsic) = signPayload.analyzeAndSign(signer, SigningMode.FEE)

        actualParsedExtrinsic.emit(parsedExtrinsic)

        extrinsicService.estimateFee(chain, extrinsic.extrinsicHex, signer)
    }

    private fun readableBytesContent(signBytesPayload: PolkadotSignPayload.Raw): String {
        return signBytesPayload.data.tryConvertHexToUtf8()
    }

    private suspend fun readableExtrinsicContent(): String {
        return extrinsicGson.toJson(actualParsedExtrinsic.first())
    }

    private suspend fun signBytes(signBytesPayload: PolkadotSignPayload.Raw): SignedResult {
        val accountId = signBytesPayload.address.anyAddressToAccountId()

        val signer = resolveWalletSigner()
        val payload = SignerPayloadRaw.fromUnsafeString(signBytesPayload.data, accountId)

        val chainId = signBytesChainResolver.resolveChainId(signBytesPayload.address)
        val signature = signer.signRaw(payload, chainId)

        return SignedResult(signature.asHexString(), modifiedTransaction = null)
    }

    private suspend fun signExtrinsic(extrinsicPayload: PolkadotSignPayload.Json): SignedResult {
        val signer = resolveWalletSigner()
        val (extrinsic, modifiedOriginal) = extrinsicPayload.analyzeAndSign(signer, SigningMode.SUBMISSION)

        val modifiedTx = if (modifiedOriginal) extrinsic.extrinsicHex else null

        return SignedResult(extrinsic.signatureHex, modifiedTx)
    }

    private suspend fun PolkadotSignPayload.Json.analyzeAndSign(
        signer: NovaSigner,
        signingMode: SigningMode
    ): ActualExtrinsic {
        val chain = chain()
        val runtime = chainRegistry.getRuntime(genesisHash)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, this)

        val actualMetadataHash = actualMetadataHash(chain, signer)

        val signingContext = signingContextFactory.default(chain)

        val extrinsic = with(parsedExtrinsic) {
            ExtrinsicBuilder(runtime, ExtrinsicVersion.V4, BatchMode.BATCH_ALL).apply {
                setTransactionExtension(CheckMortality(era, blockHash))
                setTransactionExtension(CheckGenesis(genesisHash))
                setTransactionExtension(ChargeTransactionPayment(tip))
                setTransactionExtension(CheckMetadataHash(actualMetadataHash.checkMetadataHash))
                setTransactionExtension(CheckSpecVersion(specVersion))
                setTransactionExtension(CheckTxVersion(transactionVersion))

                call(parsedExtrinsic.call)
                CustomTransactionExtensions.applyDefaultValues(builder = this)
                applyCustomSignedExtensions(parsedExtrinsic)

                signer.setSignerData(signingContext, signingMode)
            }
        }.buildExtrinsic()

        val actualParsedExtrinsic = parsedExtrinsic.copy(
            metadataHash = actualMetadataHash.checkMetadataHash.metadataHash
        )

        return ActualExtrinsic(
            signedExtrinsic = extrinsic,
            modifiedOriginal = actualMetadataHash.modifiedOriginal,
            actualParsedExtrinsic = actualParsedExtrinsic
        )
    }

    private fun SignerPayloadRaw.Companion.fromUnsafeString(data: String, signer: AccountId): SignerPayloadRaw {
        val unsafeMessage = decodeSigningMessage(data)
        val safeMessage = protectSigningMessage(unsafeMessage)

        return SignerPayloadRaw(safeMessage, signer)
    }

    private fun decodeSigningMessage(data: String): ByteArray {
        return kotlin.runCatching { data.fromHex() }.getOrElse { data.encodeToByteArray() }
    }

    private fun protectSigningMessage(message: ByteArray): ByteArray {
        val prefix = "<Bytes>".encodeToByteArray()
        val suffix = "</Bytes>".encodeToByteArray()

        if (message.startsWith(prefix) && message.endsWith(suffix)) return message

        return prefix + message + suffix
    }

    private suspend fun PolkadotSignPayload.Json.actualMetadataHash(chain: Chain, signer: NovaSigner): ActualMetadataHash {
        // If a dapp haven't declared a permission to modify extrinsic - return whatever metadataHash present in payload
        if (withSignedTransaction != true) {
            return ActualMetadataHash(modifiedOriginal = false, hexHash = metadataHash)
        }

        // If a dapp have specified metadata hash explicitly - use it
        if (metadataHash != null) {
            return ActualMetadataHash(modifiedOriginal = false, hexHash = metadataHash)
        }

        // Else generate and use our own proof
        val metadataProof = metadataShortenerService.generateMetadataProof(chain.id)
        return ActualMetadataHash(modifiedOriginal = true, checkMetadataHash = metadataProof.checkMetadataHash)
    }

    private fun PolkadotSignPayload.Json.decodedCall(runtime: RuntimeSnapshot): GenericCall.Instance {
        return GenericCall.fromHex(runtime, method)
    }

    private suspend fun PolkadotSignPayload.Json.chain(): Chain {
        return chainRegistry.getChainOrNull(genesisHash) ?: throw ExternalSignInteractor.Error.UnsupportedChain(genesisHash)
    }

    private suspend fun PolkadotSignPayload.Json.chainOrNull(): Chain? {
        return chainRegistry.getChainOrNull(genesisHash)
    }

    private fun parseDAppExtrinsic(runtime: RuntimeSnapshot, payloadJSON: PolkadotSignPayload.Json): DAppParsedExtrinsic {
        return with(payloadJSON) {
            DAppParsedExtrinsic(
                address = address,
                nonce = nonce.bigIntegerFromHex(),
                specVersion = specVersion.intFromHex(),
                transactionVersion = transactionVersion.intFromHex(),
                genesisHash = genesisHash.fromHex(),
                blockHash = blockHash.fromHex(),
                era = EraType.fromHex(runtime, era),
                tip = tip.bigIntegerFromHex(),
                call = decodedCall(runtime),
                metadataHash = metadataHash?.fromHex(),
                assetId = payloadJSON.tryDecodeAssetId(runtime)
            )
        }
    }

    private suspend fun PolkadotSignPayload.accountId(): AccountId {
        return when (this) {
            is PolkadotSignPayload.Json -> {
                val chain = chainOrNull()

                chain?.accountIdOf(address) ?: address.anyAddressToAccountId()
            }

            is PolkadotSignPayload.Raw -> address.anyAddressToAccountId()
        }
    }

    private class ActualMetadataHash(val modifiedOriginal: Boolean, val checkMetadataHash: CheckMetadataHashMode) {
        constructor(modifiedOriginal: Boolean, hash: ByteArray?) : this(modifiedOriginal, CheckMetadataHashMode(hash))

        constructor(modifiedOriginal: Boolean, hexHash: String?) : this(modifiedOriginal, hexHash?.fromHex())
    }

    private data class ActualExtrinsic(
        val signedExtrinsic: SendableExtrinsic,
        val modifiedOriginal: Boolean,
        val actualParsedExtrinsic: DAppParsedExtrinsic
    )

    private data class SignedResult(val signature: String, val modifiedTransaction: String?)

    private fun ExtrinsicBuilder.applyCustomSignedExtensions(parsedExtrinsic: DAppParsedExtrinsic): ExtrinsicBuilder {
        parsedExtrinsic.assetId?.let { chargeAssetTxPayment(it) }

        return this
    }

    private fun PolkadotSignPayload.Json.tryDecodeAssetId(runtime: RuntimeSnapshot): Any? {
        return assetId?.let(runtime::decodeCustomTxPaymentId)
    }
}

private fun CheckMetadataHashMode(hash: ByteArray?): CheckMetadataHashMode {
    return if (hash != null) {
        CheckMetadataHashMode.Enabled(hash)
    } else {
        CheckMetadataHashMode.Disabled
    }
}

private val CheckMetadataHashMode.metadataHash: ByteArray?
    get() = if (this is CheckMetadataHashMode.Enabled) hash else null
