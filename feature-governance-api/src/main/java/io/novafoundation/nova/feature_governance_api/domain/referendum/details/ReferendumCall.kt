package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId

sealed class ReferendumCall {

    open fun combineWith(other: ReferendumCall): ReferendumCall = this

    data class TreasuryRequest(val amount: Balance, val beneficiary: AccountId) : ReferendumCall() {

        override fun combineWith(other: ReferendumCall): ReferendumCall {
            if (other is TreasuryRequest) {
                return copy(amount = amount + other.amount)
            }

            return super.combineWith(other)
        }
    }
}
