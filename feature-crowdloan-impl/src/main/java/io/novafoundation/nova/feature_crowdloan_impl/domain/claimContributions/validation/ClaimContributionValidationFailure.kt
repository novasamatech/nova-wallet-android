package io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

sealed class ClaimContributionValidationFailure {

    class NotEnoughBalanceToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : ClaimContributionValidationFailure(), NotEnoughToPayFeesError
}
