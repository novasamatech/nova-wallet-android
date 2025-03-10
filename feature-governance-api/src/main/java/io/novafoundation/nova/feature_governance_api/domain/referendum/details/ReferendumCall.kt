package io.novafoundation.nova.feature_governance_api.domain.referendum.details

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

sealed class ReferendumCall {

    open fun combineWith(other: ReferendumCall): ReferendumCall = this

    data class TreasuryRequest(
        val amount: Balance,
        val beneficiary: AccountId,
        val chainAsset: Chain.Asset,
    ) : ReferendumCall() {

        override fun combineWith(other: ReferendumCall): ReferendumCall {
            if (other is TreasuryRequest && other.chainAsset.fullId == chainAsset.fullId) {
                return copy(amount = amount + other.amount)
            }

            return super.combineWith(other)
        }
    }
}
