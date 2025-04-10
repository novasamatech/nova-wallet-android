package io.novafoundation.nova.runtime.extrinsic.metadata

import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHashMode
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.chain.RuntimeVersion

class MetadataProof(
    val checkMetadataHash: CheckMetadataHashMode,
    val usedVersion: RuntimeVersion
)
