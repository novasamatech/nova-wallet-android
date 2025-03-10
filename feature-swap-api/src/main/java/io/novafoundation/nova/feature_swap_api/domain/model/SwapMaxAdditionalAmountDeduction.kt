package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

/**
 * Deductions from account balance other than those caused by fees
 */
class SwapMaxAdditionalAmountDeduction(
    val fromCountedTowardsEd: Balance
)
