package io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.validation.EmptyValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
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
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.anyAddressToAccountId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.extrinsic.signer.generateMetadataProofWithSignerRestrictions
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
import io.novasama.substrate_sdk_android.runtime.extrinsic.CheckMetadataHash
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.fromUtf8
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PolkadotSignInteractorFactory(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val metadataShortenerService: MetadataShortenerService,
    private val signerProvider: SignerProvider,
) {

    fun create(request: ExternalSignRequest.Polkadot, wallet: ExternalSignWallet) = PolkadotExternalSignInteractor(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        tokenRepository = tokenRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        wallet = wallet,
        signerProvider = signerProvider,
        metadataShortenerService = metadataShortenerService
    )
}

class PolkadotExternalSignInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val request: ExternalSignRequest.Polkadot,
    private val signerProvider: SignerProvider,
    private val metadataShortenerService: MetadataShortenerService,
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

    override fun commissionTokenFlow(): Flow<Token>? {
        val chainId = signPayload.maybeSignExtrinsic()?.genesisHash ?: return null

        return flow {
            val chain = chainRegistry.getChainOrNull(chainId) ?: return@flow

            emitAll(tokenRepository.observeToken(chain.utilityAsset))
        }
    }

    override suspend fun performOperation(upToDateFee: Fee?): ExternalSignCommunicator.Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (signPayload) {
                is PolkadotSignPayload.Json -> signExtrinsic(signPayload)
                is PolkadotSignPayload.Raw -> signBytes(signPayload)
            }
        }.fold(
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

        val signer = signPayload.feeSigner()
        val (extrinsic, _, parsedExtrinsic) = signPayload.analyzeAndSign(signer)

        actualParsedExtrinsic.emit(parsedExtrinsic)

        extrinsicService.estimateFee(chain, extrinsic.extrinsicHex, signer)
    }

    private fun readableBytesContent(signBytesPayload: PolkadotSignPayload.Raw): String {
        return signBytesPayload.data
    }

    private suspend fun readableExtrinsicContent(): String {
        return extrinsicGson.toJson(actualParsedExtrinsic.first())
    }

    private suspend fun signBytes(signBytesPayload: PolkadotSignPayload.Raw): SignedResult {
        val accountId = signBytesPayload.address.anyAddressToAccountId()

        val signer = resolveWalletSigner()
        val payload = runCatching {
            SignerPayloadRaw.fromHex(signBytesPayload.data, accountId)
        }.getOrElse {
            SignerPayloadRaw.fromUtf8(signBytesPayload.data, accountId)
        }

        val signature = signer.signRaw(payload).asHexString()
        return SignedResult(signature, modifiedTransaction = null)
    }

    private suspend fun signExtrinsic(extrinsicPayload: PolkadotSignPayload.Json): SignedResult {
        val signer = resolveWalletSigner()
        val (extrinsic, modifiedOriginal) = extrinsicPayload.analyzeAndSign(signer)

        val modifiedTx = if (modifiedOriginal) extrinsic.extrinsicHex else null

        return SignedResult(extrinsic.signatureHex, modifiedTx)
    }

    private suspend fun PolkadotSignPayload.Json.feeSigner(): FeeSigner {
        val chain = chain()

        return signerProvider.feeSigner(resolveMetaAccount(), chain)
    }

    private suspend fun PolkadotSignPayload.Json.analyzeAndSign(signer: NovaSigner): ActualExtrinsic {
        val chain = chain()
        val runtime = chainRegistry.getRuntime(genesisHash)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, this)

        val accountId = chain.accountIdOf(address)

        val actualMetadataHash = actualMetadataHash(chain, signer)

        val builder = with(parsedExtrinsic) {
            ExtrinsicBuilder(
                runtime = runtime,
                nonce = Nonce.singleTx(nonce),
                runtimeVersion = RuntimeVersion(
                    specVersion = specVersion,
                    transactionVersion = transactionVersion
                ),
                signer = signer,
                accountId = accountId,
                genesisHash = genesisHash,
                checkMetadataHash = actualMetadataHash.checkMetadataHash,
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
                blockHash = blockHash,
                era = era,
                tip = tip
            )
        }

        val extrinsic = builder.call(parsedExtrinsic.call).buildExtrinsic()

        val actualParsedExtrinsic = parsedExtrinsic.copy(
            metadataHash = actualMetadataHash.checkMetadataHash.metadataHash
        )

        return ActualExtrinsic(
            signedExtrinsic = extrinsic,
            modifiedOriginal = actualMetadataHash.modifiedOriginal,
            actualParsedExtrinsic = actualParsedExtrinsic
        )
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
        val metadataProof = metadataShortenerService.generateMetadataProofWithSignerRestrictions(chain, signer)
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
                metadataHash = metadataHash?.fromHex()
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

    private class ActualMetadataHash(val modifiedOriginal: Boolean, val checkMetadataHash: CheckMetadataHash) {

        constructor(modifiedOriginal: Boolean, hash: ByteArray?) : this(modifiedOriginal, CheckMetadataHash(hash))

        constructor(modifiedOriginal: Boolean, hexHash: String?) : this(modifiedOriginal, hexHash?.fromHex())
    }

    private data class ActualExtrinsic(
        val signedExtrinsic: SendableExtrinsic,
        val modifiedOriginal: Boolean,
        val actualParsedExtrinsic: DAppParsedExtrinsic
    )

    private data class SignedResult(val signature: String, val modifiedTransaction: String?)
}

private fun CheckMetadataHash(hash: ByteArray?): CheckMetadataHash {
    return if (hash != null) {
        CheckMetadataHash.Enabled(hash)
    } else {
        CheckMetadataHash.Disabled
    }
}

private val CheckMetadataHash.metadataHash: ByteArray?
    get() = if (this is CheckMetadataHash.Enabled) hash else null
