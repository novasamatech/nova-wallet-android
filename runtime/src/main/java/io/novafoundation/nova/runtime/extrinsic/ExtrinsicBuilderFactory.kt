package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.mapper.toRuntimeVersion
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersion
import java.math.BigInteger

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
        signer: NovaSigner,
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
        signer: NovaSigner,
        chain: Chain,
    ): Sequence<ExtrinsicBuilder> {
        return createMulti(chain, signer, signer.signerAccountId(chain))
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

        val baseNonce = rpcCalls.getNonce(chain.id, accountAddress)
        var nonceOffset = BigInteger.ZERO

        return generateSequence {
            val newElement = ExtrinsicBuilder(
                tip = chain.additional?.defaultTip.orZero(),
                runtime = runtime,
                nonce = Nonce(baseNonce, nonceOffset),
                runtimeVersion = runtimeVersion,
                genesisHash = chain.requireGenesisHash().fromHex(),
                blockHash = mortality.blockHash.fromHex(),
                era = mortality.era,
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
                signer = signer,
                accountId = accountId
            )

            nonceOffset++

            newElement
        }
    }

    private suspend fun getRuntimeVersion(chain: Chain): RuntimeVersion {
        return chainDao.runtimeInfo(chain.id)?.toRuntimeVersion() ?: rpcCalls.getRuntimeVersion(chain.id)
    }
}
