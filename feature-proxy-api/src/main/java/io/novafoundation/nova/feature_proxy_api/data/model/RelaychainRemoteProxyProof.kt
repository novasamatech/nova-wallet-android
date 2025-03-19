package io.novafoundation.nova.feature_proxy_api.data.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class RelaychainRemoteProxyProof(
    val proofBlock: BlockNumber,
    val proof: List<ByteArray>
)
