package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.utils.bigIntegerFromHex
import io.novafoundation.nova.common.utils.intFromHex
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.secrets.getKeypair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.extensions.fromHex
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
            val call = decodeCall(signerPayload)

            // assumption - extension has access only to selected meta account
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = chainRegistry.getChain(signerPayload.genesisHash)
            val accountId = chain.accountIdOf(signerPayload.address)

            val keypair = secretStoreV2.getKeypair(metaAccount, chain, accountId)
            val runtime = chainRegistry.getRuntime(chain.id)

            val extrinsicBuilder = ExtrinsicBuilder(
                runtime = runtime,
                keypair = keypair,
                nonce = signerPayload.nonce.bigIntegerFromHex(),
                runtimeVersion = RuntimeVersion(
                    specVersion = signerPayload.specVersion.intFromHex(),
                    transactionVersion = signerPayload.transactionVersion.intFromHex()
                ),
                genesisHash = signerPayload.genesisHash.fromHex(),
                multiChainEncryption = metaAccount.multiChainEncryptionIn(chain),
                accountIdentifier = AddressInstanceConstructor.constructInstance(runtime.typeRegistry, accountId),
                blockHash = signerPayload.blockHash.fromHex(),
                era = EraType.fromHex(runtime, signerPayload.era),
                tip = signerPayload.tip.bigIntegerFromHex()
            )

            extrinsicBuilder.call(call)

            extrinsicBuilder.buildSignature()
        }
    }

    suspend fun calculateFee(signerPayload: SignerPayloadJSON): BigInteger = withContext(Dispatchers.Default) {
        val call = decodeCall(signerPayload)
        val chain = chainRegistry.getChain(signerPayload.genesisHash)

        extrinsicService.estimateFee(chain) {
            call(call)
        }
    }

    private suspend fun decodeCall(signerPayload: SignerPayloadJSON): GenericCall.Instance {
        val runtime = chainRegistry.getRuntime(signerPayload.genesisHash)

        return GenericCall.fromHex(runtime, signerPayload.method)
    }
}
