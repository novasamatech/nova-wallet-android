package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

sealed class XcmTransferDryRunOrigin {

    /**
     * Use fake signed origin that will be topped up to perform the dry run
     * Useful for dry running as the part of fee calculation process
     */
    data object Fake : XcmTransferDryRunOrigin()

    /**
     * Use [accountId] as a origin for simulation. Simulation will be done on the current state of the account,
     * without preliminary top ups e.t.c.
     * [crossChainFee] will be added to the transfer amount
     * Useful for final dry run, when all transfer parameters are known and finalized
     */
    class Signed(val accountId: AccountIdKey, val crossChainFee: Balance) : XcmTransferDryRunOrigin()
}
