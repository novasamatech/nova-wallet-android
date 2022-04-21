package io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.sign

import com.google.gson.Gson
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.secrets.getKeypair
import io.novafoundation.nova.feature_account_api.data.secrets.signSubstrate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.PolkadotJsSignRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.maybeSignExtrinsic
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
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.EraType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic.EncodingInstance.CallRepresentation
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
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
    private val secretStoreV2: SecretStoreV2,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
) {

    fun create(request: PolkadotJsSignRequest) = PolkadotJsSignInteractor(
        extrinsicService = extrinsicService,
        chainRegistry = chainRegistry,
        accountRepository = accountRepository,
        secretStoreV2 = secretStoreV2,
        tokenRepository = tokenRepository,
        extrinsicGson = extrinsicGson,
        addressIconGenerator = addressIconGenerator,
        request = request
    )
}

class PolkadotJsSignInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val tokenRepository: TokenRepository,
    private val extrinsicGson: Gson,
    private val addressIconGenerator: AddressIconGenerator,
    private val request: PolkadotJsSignRequest,
) : DAppSignInteractor {

    val signerPayload = request.payload

    override suspend fun createAccountAddressModel(): AddressModel {
        return addressIconGenerator.createAddressModel(
            accountAddress = signerPayload.address,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            accountName = null,
            background = AddressIconGenerator.BACKGROUND_TRANSPARENT
        )
    }

    override suspend fun chainUi(): ChainUi? {
        return signerPayload.maybeSignExtrinsic()?.let {
            mapChainToUi(it.chain())
        }
    }

    override fun commissionTokenFlow(): Flow<Token>? {
        val chainId = signerPayload.maybeSignExtrinsic()?.genesisHash ?: return null

        return flow {
            val chain = chainRegistry.getChainOrNull(chainId) ?: return@flow

            emitAll(tokenRepository.observeToken(chain.utilityAsset))
        }
    }

    override suspend fun performOperation(): Response = withContext(Dispatchers.Default) {
        runCatching {
            when (signerPayload) {
                is SignerPayload.Json -> signExtrinsic(signerPayload)
                is SignerPayload.Raw -> signBytes(signerPayload)
            }
        }.fold(
            onSuccess = { signature ->
                Response.Signed(request.id, signature)
            },
            onFailure = {
                Response.SigningFailed(request.id)
            }
        )
    }

    override suspend fun readableOperationContent(): String = withContext(Dispatchers.Default) {
        when (signerPayload) {
            is SignerPayload.Json -> readableExtrinsicContent(signerPayload)
            is SignerPayload.Raw -> readableBytesContent(signerPayload)
        }
    }

    override suspend fun calculateFee(): BigInteger = withContext(Dispatchers.Default) {
        require(signerPayload is SignerPayload.Json)

        val extrinsicBuilder = signerPayload.toExtrinsicBuilderWithoutCall()
        val runtime = chainRegistry.getRuntime(signerPayload.chain().id)

        val extrinsic = when (val callRepresentation = signerPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).build()
            is CallRepresentation.Bytes -> extrinsicBuilder.build(rawCallBytes = callRepresentation.bytes)
        }

        extrinsicService.estimateFee(signerPayload.chain().id, extrinsic)
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

        return secretStoreV2.signSubstrate(
            metaAccount = metaAccount,
            accountId = substrateAccountId,
            message = signBytesPayload.data.fromHex()
        ).toHexString(withPrefix = true)
    }

    private suspend fun signExtrinsic(extrinsicPayload: SignerPayload.Json): String {
        val runtime = chainRegistry.getRuntime(extrinsicPayload.chain().id)
        val extrinsicBuilder = extrinsicPayload.toExtrinsicBuilderWithoutCall()

        return when (val callRepresentation = extrinsicPayload.callRepresentation(runtime)) {
            is CallRepresentation.Instance -> extrinsicBuilder.call(callRepresentation.call).buildSignature()
            is CallRepresentation.Bytes -> extrinsicBuilder.buildSignature(rawCallBytes = callRepresentation.bytes)
        }
    }

    private suspend fun SignerPayload.Json.toExtrinsicBuilderWithoutCall(): ExtrinsicBuilder {
        val chain = chain()
        val runtime = chainRegistry.getRuntime(genesisHash)
        val parsedExtrinsic = parseDAppExtrinsic(runtime, this)

        // assumption - extension has access only to selected meta account
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val accountId = chain.accountIdOf(address)

        val keypair = secretStoreV2.getKeypair(metaAccount, chain, accountId)

        return with(parsedExtrinsic) {
            ExtrinsicBuilder(
                runtime = runtime,
                keypair = keypair,
                nonce = nonce,
                runtimeVersion = RuntimeVersion(
                    specVersion = specVersion,
                    transactionVersion = transactionVersion
                ),
                genesisHash = genesisHash,
                multiChainEncryption = metaAccount.multiChainEncryptionIn(chain),
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(runtime),
                accountIdentifier = AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
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
        return chainRegistry.getChain(genesisHash)
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
