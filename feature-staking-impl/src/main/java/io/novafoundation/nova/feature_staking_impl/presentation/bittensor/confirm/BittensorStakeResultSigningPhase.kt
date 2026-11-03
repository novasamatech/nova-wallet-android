package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The result sheet is a dialog above the screen, and a Ledger, Vault or signatory screen is added on top of the screen
 * rather than replacing it, so the sheet has to go while [signingOutsideApp] and come back with the next result.
 */
fun Flow<BittensorStakeResultUi?>.hiddenWhileSigningOutsideApp(signingOutsideApp: Flow<Boolean>): Flow<BittensorStakeResultUi?> {
    return combine(this, signingOutsideApp) { result, signing -> result.takeUnless { signing } }
}
