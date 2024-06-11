package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataProof
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce
import java.math.BigInteger

class ExtrinsicBuilderFactory(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val mortalityConstructor: MortalityConstructor,
    private val metadataShortenerService: MetadataShortenerService,
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
        signer: NovaSigner,
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
        signer: NovaSigner,
        accountId: AccountId,
    ): Sequence<ExtrinsicBuilder> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val accountAddress = chain.addressOf(accountId)

        val mortality = mortalityConstructor.constructMortality(chain.id)

        val baseNonce = rpcCalls.getNonce(chain.id, accountAddress)
        var nonceOffset = BigInteger.ZERO

        val metadataProof = metadataShortenerService.generateMetadataProofWithSignerRestrictions(chain, signer)

        return generateSequence {
            val newElement = ExtrinsicBuilder(
                tip = chain.additional?.defaultTip.orZero(),
                runtime = runtime,
                nonce = Nonce(baseNonce, nonceOffset),
                runtimeVersion = metadataProof.usedVersion,
                genesisHash = chain.requireGenesisHash().fromHex(),
                blockHash = mortality.blockHash.fromHex(),
                era = mortality.era,
                customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
                checkMetadataHash = metadataProof.checkMetadataHash,
                signer = signer,
                accountId = accountId
            )

            nonceOffset++

            newElement
        }
    }

    private suspend fun MetadataShortenerService.generateMetadataProofWithSignerRestrictions(
        chain: Chain,
        signer: NovaSigner,
    ): MetadataProof {
        return if (signer.supportsCheckMetadataHash(chain)) {
            generateMetadataProof(chain.id)
        } else {
            generateDisabledMetadataProof(chain.id)
        }
    }
}
