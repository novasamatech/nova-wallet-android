package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.secrets.getKeypair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.EraType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.instances.AddressInstanceConstructor
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DappSignExtrinsicInteractor(
    private val extrinsicService: ExtrinsicService,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val secretStoreV2: SecretStoreV2
) {

    suspend fun buildSignature(signerPayload: SignerPayloadJSON): Result<String> = withContext(Dispatchers.Default) {
        kotlin.runCatching {
            signerPayload.toExtrinsicBuilder().buildSignature()
        }
    }

    suspend fun calculateFee(signerPayload: SignerPayloadJSON): BigInteger = withContext(Dispatchers.Default) {
        val extrinsic = signerPayload.toExtrinsicBuilder().build()

        extrinsicService.estimateFee(signerPayload.chain().id, extrinsic)
    }

    private suspend fun SignerPayloadJSON.toExtrinsicBuilder(): ExtrinsicBuilder {
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
            ).also { it.call(call) }
        }
    }

    private suspend fun SignerPayloadJSON.chain(): Chain {
        return chainRegistry.getChain(genesisHash)
    }

    private fun parseDAppExtrinsic(runtime: RuntimeSnapshot, payloadJSON: SignerPayloadJSON): DAppParsedExtrinsic {
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
                call = GenericCall.fromHex(runtime, method)
            )
        }
    }
}
