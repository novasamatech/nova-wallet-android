package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.feeSigner.FeeSigner
import io.novafoundation.nova.runtime.mapper.toRuntimeVersion
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.Signer
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion

class ExtrinsicBuilderFactory(
    private val chainDao: ChainDao,
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val mortalityConstructor: MortalityConstructor,
) {

    /**
     * Create with special signer for fee calculation
     */
    suspend fun createForFee(
        signer: FeeSigner,
        chain: Chain,
    ): ExtrinsicBuilder {
        return createMultiForFee(signer, chain).first()
    }

    /**
     * Create with real keypair
     */
    suspend fun create(
        chain: Chain,
        signer: Signer,
        accountId: AccountId,
    ): ExtrinsicBuilder {
        return createMulti(chain, signer, accountId).first()
    }

    suspend fun createMultiForFee(
        signer: FeeSigner,
        chain: Chain,
    ): Sequence<ExtrinsicBuilder> {
        return createMulti(chain, signer, signer.accountId())
    }

    suspend fun createMulti(
        chain: Chain,
        signer: Signer,
        accountId: AccountId,
    ): Sequence<ExtrinsicBuilder> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountAddress = chain.addressOf(accountId)

        val runtimeVersion = getRuntimeVersion(chain)
        val mortality = mortalityConstructor.constructMortality(chain.id)

        var nonce = rpcCalls.getNonce(chain.id, accountAddress)

        return generateSequence {
            val newElement = ExtrinsicBuilder(
                tip = chain.additional?.defaultTip.orZero(),
                runtime = runtime,
                nonce = nonce,
                runtimeVersion = runtimeVersion,
                genesisHash = chain.requireGenesisHash().fromHex(),
                blockHash = mortality.blockHash.fromHex(),
                era = mortality.era,
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
                signer = signer,
                accountId = accountId
            )

            nonce++

            newElement
        }
    }

    private suspend fun getRuntimeVersion(chain: Chain): RuntimeVersion {
        return chainDao.runtimeInfo(chain.id)?.toRuntimeVersion() ?: rpcCalls.getRuntimeVersion(chain.id)
    }
}
