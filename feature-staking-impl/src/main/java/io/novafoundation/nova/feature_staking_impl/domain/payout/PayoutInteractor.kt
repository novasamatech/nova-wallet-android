package io.novafoundation.nova.feature_staking_impl.domain.payout

import io.novafoundation.nova.common.utils.hasCall
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.model.Payout
import io.novafoundation.nova.feature_staking_impl.domain.validations.payout.MakePayoutPayload
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.state.chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class PayoutInteractor(
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService
) {

    suspend fun estimatePayoutFee(payouts: List<Payout>): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateMultiFee(stakingSharedState.chain()) {
                payoutMultiple(payouts)
            }
        }
    }

    suspend fun makePayouts(payload: MakePayoutPayload): RetriableMultiResult<ExtrinsicStatus.InBlock> {
        return withContext(Dispatchers.IO) {
            val chain = stakingSharedState.chain()
            val accountId = chain.accountIdOf(payload.originAddress)
            val origin = TransactionOrigin.WalletWithAccount(accountId)

            extrinsicService.submitMultiExtrinsicAwaitingInclusion(chain, origin) {
                payoutMultiple(payload.payouts)
            }
        }
    }

    private fun CallBuilder.payoutMultiple(payouts: List<Payout>) {
        payouts.forEach { payout ->
            makePayout(payout)
        }
    }

    private fun CallBuilder.makePayout(payout: Payout) {
        if (runtime.metadata.staking().hasCall("payout_stakers_by_page")) {
            payout.pagesToClaim.onEach { page ->
                payoutStakersByPage(payout.era, payout.validatorStash.value, page)
            }
        } else {
            // paged payout is not present so we use regular one
            payoutStakers(payout.era, payout.validatorStash.value)
        }
    }

    private fun CallBuilder.payoutStakers(era: BigInteger, validatorId: AccountId) {
        addCall(
            "Staking",
            "payout_stakers",
            mapOf(
                "validator_stash" to validatorId,
                "era" to era
            )
        )
    }

    private fun CallBuilder.payoutStakersByPage(era: BigInteger, validatorId: AccountId, page: Int) {
        addCall(
            "Staking",
            "payout_stakers",
            mapOf(
                "validator_stash" to validatorId,
                "era" to era,
                "page" to page.toBigInteger()
            )
        )
    }
}
