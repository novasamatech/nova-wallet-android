package io.novafoundation.nova.runtime.extrinsic.metadata

import io.novasama.substrate_sdk_android.runtime.extrinsic.CheckMetadataHash
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersion

class MetadataProof(
    val checkMetadataHash: CheckMetadataHash,
    val usedVersion: RuntimeVersion
)
