package io.novafoundation.nova.feature_account_api.data.multisig.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber

class MultisigTimePoint(
    val height: BlockNumber,
    val extrinsicIndex: Int
) 
