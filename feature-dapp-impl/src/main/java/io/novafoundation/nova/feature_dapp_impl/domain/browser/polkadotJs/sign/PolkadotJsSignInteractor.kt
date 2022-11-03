package io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.sign

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.utils.asHexString
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.EmptyValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationFailure
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationSystem
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.convertingToAmount
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.failedSigningIfNotCancelled
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.PolkadotJsSignRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.maybeSignExtrinsic
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
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
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.math.BigInteger

class PolkadotJsSignInteractorFactory(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val walletRepository: WalletRepository,
    private val signerProvider: SignerProvider,
) {

    fun create(request: PolkadotJsSignRequest) = PolkadotJsSignInteractor(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        tokenRepository = tokenRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request,
        walletRepository = walletRepository,
        signerProvider = signerProvider
    )
}

class PolkadotJsSignInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val request: PolkadotJsSignRequest,
    private val walletRepository: WalletRepository,
    private val signerProvider: SignerProvider
) : DAppSignInteractor {

    val signerPayload = request.payload

    override val validationSystem: ConfirmDAppOperationValidationSystem = when (signerPayload) {
        is SignerPayload.Json -> operationValidationSystem(signerPayload)
        is SignerPayload.Raw -> EmptyValidationSystem()
    }

    override suspend fun createAccountAddressModel(): AddressModel {
        return addressIconGenerator.createAddressModel(
            accountAddress = signerPayload.address,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            accountName = null,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
    }

    override suspend fun chainUi(): Result<ChainUi?> {
        return runCatching {
            signerPayload.maybeSignExtrinsic()?.let {
                mapChainToUi(it.chain())
            }
        }
    }

    override fun commissionTokenFlow(): Flow<Token>? {
        val chainId = signerPayload.maybeSignExtrinsic()?.genesisHash ?: return null

        return flow {
            val chain = chainRegistry.getChainOrNull(chainId) ?: return@flow

            emitAll(tokenRepository.observeToken(chain.utilityAsset))
        }
    }

    override suspend fun performOperation(): Response? = withContext(Dispatchers.Default) {
        runCatching {
            when (signerPayload) {
                is SignerPayload.Json -> signExtrinsic(signerPayload)
                is SignerPayload.Raw -> signBytes(signerPayload)
            }
        }.fold(
            onSuccess = { signature ->
                Response.Signed(request.id, signature)
            },
            onFailure = { error ->
                error.failedSigningIfNotCancelled(request.id)
            }
        )
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (signerPayload) {
            is SignerPayload.Json -> readableExtrinsicContent(signerPayload)
            is SignerPayload.Raw -> readableBytesContent(signerPayload)
        }
    }

    override suspend fun calculateFee(): BigInteger? = withContext(Dispatchers.Default) {
        require(signerPayload is SignerPayload.Json)

        val chain = signerPayload.chainOrNull() ?: return@withContext null

        val extrinsicBuilder = signerPayload.toExtrinsicBuilderWithoutCall(forFee = true)
        val runtime = chainRegistry.getRuntime(chain.id)

        val extrinsic = when (val callRepresentation = signerPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).build()
            is CallRepresentation.Bytes -> extrinsicBuilder.build(rawCallBytes = callRepresentation.bytes)
        }

        extrinsicService.estimateFee(chain.id, extrinsic)
    }

    private fun operationValidationSystem(operationPayload: SignerPayload.Json): ConfirmDAppOperationValidationSystem = ValidationSystem {
        // since we don't know how arbitrary extrinsic gonna affect transferable balance we only check for fees
        sufficientBalance(
            fee = { it.convertingToAmount { calculateFee().orZero() } },
            available = {
                val asset = walletRepository.getAsset(
                    metaId = accountRepository.getSelectedMetaAccount().id,
                    chainAsset = operationPayload.chain().utilityAsset
                )!!

                asset.transferable
            },
            error = { _, _ -> ConfirmDAppOperationValidationFailure.NotEnoughBalanceToPayFees }
        )
    }

    private fun readableBytesContent(signBytesPayload: SignerPayload.Raw): String {
        return signBytesPayload.data
    }

    private suspend fun readableExtrinsicContent(extrinsicPayload: SignerPayload.Json): String {
        val runtime = chainRegistry.getRuntime(extrinsicPayload.chain().id)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, extrinsicPayload)

        return extrinsicGson.toJson(parsedExtrinsic)
    }

    private suspend fun signBytes(signBytesPayload: SignerPayload.Raw): String {
        // assumption - only substrate dApps
        val substrateAccountId = signBytesPayload.address.toAccountId()

        // assumption - extension has access only to selected meta account
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val signer = signerProvider.signerFor(metaAccount)
        val payload = SignerPayloadRaw.fromHex(signBytesPayload.data, substrateAccountId)

        return signer.signRaw(payload).asHexString()
    }

    private suspend fun signExtrinsic(extrinsicPayload: SignerPayload.Json): String {
        val runtime = chainRegistry.getRuntime(extrinsicPayload.chain().id)
        val extrinsicBuilder = extrinsicPayload.toExtrinsicBuilderWithoutCall(forFee = false)

        return when (val callRepresentation = extrinsicPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).buildSignature()
            is CallRepresentation.Bytes -> extrinsicBuilder.buildSignature(rawCallBytes = callRepresentation.bytes)
        }
    }

    private suspend fun SignerPayload.Json.toExtrinsicBuilderWithoutCall(
        forFee: Boolean
    ): ExtrinsicBuilder {
        val chain = chain()
        val runtime = chainRegistry.getRuntime(genesisHash)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, this)

        // assumption - extension has access only to selected meta account
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = chain.accountIdOf(address)

        val signer = if (forFee) {
            signerProvider.feeSigner(chain)
        } else {
            signerProvider.signerFor(metaAccount)
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
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(runtime),
                blockHash = blockHash,
                era = era,
                tip = tip
            )
        }
    }

    private fun SignerPayload.Json.callRepresentation(runtime: RuntimeSnapshot): CallRepresentation = runCatching {
        CallRepresentation.Instance(GenericCall.fromHex(runtime, method))
    }.getOrDefault(CallRepresentation.Bytes(method.fromHex()))

    private suspend fun SignerPayload.Json.chain(): Chain {
        return chainRegistry.getChainOrNull(genesisHash) ?: throw DAppSignInteractor.Error.UnsupportedChain(genesisHash)
    }

    private suspend fun SignerPayload.Json.chainOrNull(): Chain? {
        return chainRegistry.getChainOrNull(genesisHash)
    }

    private fun parseDAppExtrinsic(runtime: RuntimeSnapshot, payloadJSON: SignerPayload.Json): DAppParsedExtrinsic {
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
