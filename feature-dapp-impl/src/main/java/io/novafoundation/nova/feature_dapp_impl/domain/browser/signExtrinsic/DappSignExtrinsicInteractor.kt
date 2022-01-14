package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import com.google.gson.Gson
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.secrets.getKeypair
import io.novafoundation.nova.feature_account_api.data.secrets.signSubstrate
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayload
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DappSignExtrinsicInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val secretStoreV2: SecretStoreV2,
    private val extrinsicGson: Gson,
) {

    suspend fun buildSignature(signerPayload: SignerPayload): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            when (signerPayload) {
                is SignerPayload.Json -> signExtrinsic(signerPayload)
                is SignerPayload.Raw -> signBytes(signerPayload)
            }
        }
    }

    suspend fun readableSignContent(signerPayload: SignerPayload): String = withContext(Dispatchers.Default) {
        when (signerPayload) {
            is SignerPayload.Json -> readableExtrinsicContent(signerPayload)
            is SignerPayload.Raw -> readableBytesContent(signerPayload)
        }
    }

    suspend fun calculateFee(signerPayload: SignerPayload.Json): BigInteger = withContext(Dispatchers.Default) {
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
