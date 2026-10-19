package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaims
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

data class BittensorRootClaimState(
    val metaId: Long,
    val coldkey: AccountIdKey,
    val chain: Chain,
    val asset: Asset,
    val claims: BittensorRootClaims,
    val supportsImmediateOperations: Boolean,
    val selectedHotkey: AccountIdKey? = null,
    val dataStatus: BittensorDataStatus = BittensorDataStatus.Fresh,
    val pendingHotkeys: Set<AccountIdKey> = emptySet()
) {

    val availableClaims: List<BittensorRootClaimPreview>
        get() = claims.claimable.filterNot { it.hotkey in pendingHotkeys }

    val selectedPreview: BittensorRootClaimPreview?
        get() = if (selectedHotkey == null) availableClaims.firstOrNull() else availableClaims.singleOrNull { it.hotkey == selectedHotkey }
}

data class BittensorRootClaimDraft(
    val state: BittensorRootClaimState,
    val preview: BittensorRootClaimPreview,
    val operation: BittensorRootClaimOperation,
    val fee: Fee
)
