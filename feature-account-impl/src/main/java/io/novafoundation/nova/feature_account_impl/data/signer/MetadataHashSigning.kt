package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataProof
import io.novafoundation.nova.runtime.extrinsic.metadata.MetadataShortenerService
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

suspend fun MetadataShortenerService.generateMetadataProofWithSignerRestrictions(
    chain: Chain,
    signerSupportsCheckMetadataHash: Boolean,
): MetadataProof {
    return if (signerSupportsCheckMetadataHash) {
        generateMetadataProof(chain.id)
    } else {
        generateDisabledMetadataProof(chain.id)
    }
}
