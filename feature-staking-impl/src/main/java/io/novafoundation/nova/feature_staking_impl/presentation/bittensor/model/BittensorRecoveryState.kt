package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicRecoveryState

data class BittensorRecoveryRequest(val metaId: Long, val chainId: String)

data class BittensorWalletRecovery(val metaId: Long, val state: ExtrinsicRecoveryState)
