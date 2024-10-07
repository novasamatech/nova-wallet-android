package io.novafoundation.nova.runtime.extrinsic.signer

import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataProof
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun MetadataShortenerService.generateMetadataProofWithSignerRestrictions(
    chain: Chain,
    signer: NovaSigner,
): MetadataProof {
    return if (signer.supportsCheckMetadataHash(chain)) {
        generateMetadataProof(chain.id)
    } else {
        generateDisabledMetadataProof(chain.id)
    }
}
