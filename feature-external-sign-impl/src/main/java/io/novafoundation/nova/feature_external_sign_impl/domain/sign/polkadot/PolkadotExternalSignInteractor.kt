package io.novafoundation.nova.feature_external_sign_impl.domain.sign.polkadot

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
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
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getChainOrNull
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.EraType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadRaw
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.fromHex
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.fromUtf8
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PolkadotSignInteractorFactory(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
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
        signerProvider = signerProvider
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
    wallet: ExternalSignWallet,
    accountRepository: AccountRepository
) : BaseExternalSignInteractor(accountRepository, wallet, signerProvider) {

    private val signPayload = request.payload

    override val validationSystem: ConfirmDAppOperationValidationSystem = EmptyValidationSystem()

    override suspend fun createAccountAddressModel(): AddressModel {
        return addressIconGenerator.createAddressModel(
            accountAddress = signPayload.address,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            accountName = null,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
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
            onSuccess = { signature ->
                ExternalSignCommunicator.Response.Signed(request.id, signature)
            },
            onFailure = { error ->
                error.failedSigningIfNotCancelled(request.id)
            }
        )
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (signPayload) {
            is PolkadotSignPayload.Json -> readableExtrinsicContent(signPayload)
            is PolkadotSignPayload.Raw -> readableBytesContent(signPayload)
        }
    }

    override suspend fun calculateFee(): Fee? = withContext(Dispatchers.Default) {
        require(signPayload is PolkadotSignPayload.Json)

        val chain = signPayload.chainOrNull() ?: return@withContext null

        val extrinsicBuilder = signPayload.toExtrinsicBuilderWithoutCall(forFee = true)
        val runtime = chainRegistry.getRuntime(chain.id)

        val extrinsic = when (val callRepresentation = signPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).build()
            is CallRepresentation.Bytes -> extrinsicBuilder.build(rawCallBytes = callRepresentation.bytes)
        }

        extrinsicService.estimateFee(chain, extrinsic)
    }

    private fun readableBytesContent(signBytesPayload: PolkadotSignPayload.Raw): String {
        return signBytesPayload.data
    }

    private suspend fun readableExtrinsicContent(extrinsicPayload: PolkadotSignPayload.Json): String {
        val runtime = chainRegistry.getRuntime(extrinsicPayload.chain().id)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, extrinsicPayload)

        return extrinsicGson.toJson(parsedExtrinsic)
    }

    private suspend fun signBytes(signBytesPayload: PolkadotSignPayload.Raw): String {
        // assumption - only substrate dApps
        val substrateAccountId = signBytesPayload.address.toAccountId()

        val signer = resolveWalletSigner()
        val payload = runCatching {
            SignerPayloadRaw.fromHex(signBytesPayload.data, substrateAccountId)
        }.getOrElse {
            SignerPayloadRaw.fromUtf8(signBytesPayload.data, substrateAccountId)
        }

        return signer.signRaw(payload).asHexString()
    }

    private suspend fun signExtrinsic(extrinsicPayload: PolkadotSignPayload.Json): String {
        val runtime = chainRegistry.getRuntime(extrinsicPayload.chain().id)
        val extrinsicBuilder = extrinsicPayload.toExtrinsicBuilderWithoutCall(forFee = false)

        return when (val callRepresentation = extrinsicPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).buildSignature()
            is CallRepresentation.Bytes -> extrinsicBuilder.buildSignature(rawCallBytes = callRepresentation.bytes)
        }
    }

    private suspend fun PolkadotSignPayload.Json.toExtrinsicBuilderWithoutCall(
        forFee: Boolean
    ): ExtrinsicBuilder {
        val chain = chain()
        val runtime = chainRegistry.getRuntime(genesisHash)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, this)

        val accountId = chain.accountIdOf(address)

        val signer = if (forFee) {
            signerProvider.feeSigner(resolveMetaAccount(), chain)
        } else {
            resolveWalletSigner()
        }

        return with(parsedExtrinsic) {
            ExtrinsicBuilder(
                runtime = runtime,
                nonce = nonce,
                runtimeVersion = RuntimeVersion(
                    specVersion = specVersion,
                    transactionVersion = transactionVersion
                ),
                signer = signer,
                accountId = accountId,
                genesisHash = genesisHash,
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
                blockHash = blockHash,
                era = era,
                tip = tip
            )
        }
    }

    private fun PolkadotSignPayload.Json.callRepresentation(runtime: RuntimeSnapshot): CallRepresentation = runCatching {
        CallRepresentation.Instance(GenericCall.fromHex(runtime, method))
    }.getOrDefault(CallRepresentation.Bytes(method.fromHex()))

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
                call = callRepresentation(runtime)
            )
        }
    }
}
