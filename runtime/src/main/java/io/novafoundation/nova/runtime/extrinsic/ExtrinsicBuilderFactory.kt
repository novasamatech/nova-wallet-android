package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.extrinsic.metadata.generateMetadataProofWithSignerRestrictions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.ChargeTransactionPayment
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckGenesis
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckMortality
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckSpecVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckTxVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHash

class ExtrinsicBuilderFactory(
    private val chainRegistry: ChainRegistry,
    private val mortalityConstructor: MortalityConstructor,
    private val metadataShortenerService: MetadataShortenerService,
) {

    class Options(
        val batchMode: BatchMode,
    )

    suspend fun create(
        chain: Chain,
        supportsCheckMetadataHash: Boolean,
        options: Options,
    ): ExtrinsicBuilder {
        return createMulti(chain, supportsCheckMetadataHash, options).first()
    }

    suspend fun createMulti(
        chain: Chain,
        supportsCheckMetadataHash: Boolean,
        options: Options,
    ): Sequence<ExtrinsicBuilder> {
        val runtime = chainRegistry.getRuntime(chain.id)

        val mortality = mortalityConstructor.constructMortality(chain.id)

        val metadataProof = metadataShortenerService.generateMetadataProofWithSignerRestrictions(chain, supportsCheckMetadataHash)

        return generateSequence {
            ExtrinsicBuilder(
                runtime = runtime,
                extrinsicVersion = ExtrinsicVersion.V4,
                batchMode = options.batchMode,
            ).apply {
                setTransactionExtension(CheckMortality(mortality.era, mortality.blockHash.fromHex()))
                setTransactionExtension(CheckGenesis(chain.requireGenesisHash().fromHex()))
                setTransactionExtension(ChargeTransactionPayment(chain.additional?.defaultTip.orZero()))
                setTransactionExtension(CheckMetadataHash(metadataProof.checkMetadataHash))
                setTransactionExtension(CheckSpecVersion(metadataProof.usedVersion.specVersion))
                setTransactionExtension(CheckTxVersion(metadataProof.usedVersion.transactionVersion))

                CustomTransactionExtensions.defaultValues().forEach(::setTransactionExtension)
            }
        }
    }
}
