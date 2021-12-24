package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerPayloadJSON
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class DappSignExtrinsicInteractor(
    private val extrinsicService: ExtrinsicService,
    private val chainRegistry: ChainRegistry,
) {

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
