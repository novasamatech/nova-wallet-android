package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer

class ExtrinsicBuilderFactory(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val mortalityConstructor: MortalityConstructor,
) {

    /**
     * Create with special signer for fee calculation
     */
    suspend fun createForFee(
        chain: Chain,
    ): ExtrinsicBuilder {
        val signer = FeeSigner(chain)

        return create(chain, signer, signer.accountId())
    }

    /**
     * Create with real keypair
     */
    suspend fun create(
        chain: Chain,
        signer: Signer,
        accountId: AccountId,
    ): ExtrinsicBuilder {
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountAddress = chain.addressOf(accountId)

        val nonce = rpcCalls.getNonce(chain.id, accountAddress)
        val runtimeVersion = rpcCalls.getRuntimeVersion(chain.id)
        val mortality = mortalityConstructor.constructMortality(chain.id)

        return ExtrinsicBuilder(
            tip = chain.additional?.defaultTip.orZero(),
            runtime = runtime,
            nonce = nonce,
            runtimeVersion = runtimeVersion,
            genesisHash = chain.genesisHash.fromHex(),
            blockHash = mortality.blockHash.fromHex(),
            era = mortality.era,
            customSignedExtensions = CustomSignedExtensions.extensionsWithValues(runtime),
            signer = signer,
            accountId = accountId
        )
    }
}
