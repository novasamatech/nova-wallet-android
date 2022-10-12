package io.novafoundation.nova.feature_governance_api.data.network.blockhain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

class TreasuryProposal(
    val id: Id,
    val proposer: AccountId,
    val amount: Balance,
    val beneficiary: AccountId,
    val bond: Balance
) {

    @JvmInline
    value class Id(val value: BigInteger)
}
