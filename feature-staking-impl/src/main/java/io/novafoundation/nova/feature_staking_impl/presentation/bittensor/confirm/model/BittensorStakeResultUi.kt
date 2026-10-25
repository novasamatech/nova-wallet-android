package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

sealed interface BittensorStakeResultUi {

    /** Result sheet buttons in display order; the last one is the primary button. */
    val actions: List<BittensorResultAction>

    data class Unconfirmed(
        val title: String,
        val description: String,
        val transactionHash: String?,
        override val actions: List<BittensorResultAction> = emptyList()
    ) : BittensorStakeResultUi

    data class Progress(
        val title: String,
        val description: String
    ) : BittensorStakeResultUi {

        override val actions: List<BittensorResultAction>
            get() = emptyList()
    }

    data class Success(
        val title: String,
        val description: String,
        val validator: String?,
        val fee: BittensorFeeUi,
        val outcome: BittensorOperationOutcome? = null,
        val amount: AmountModel? = null,
        val stakeAfter: String? = null,
        override val actions: List<BittensorResultAction> = emptyList()
    ) : BittensorStakeResultUi

    data class Failure(
        val title: String,
        val description: String,
        val error: String,
        val canRetry: Boolean = true,
        val fee: BittensorFeeUi? = null
    ) : BittensorStakeResultUi {

        /** Close, plus Try again when a retry can succeed. */
        override val actions: List<BittensorResultAction>
            get() = if (canRetry) listOf(BittensorResultAction.CLOSE, BittensorResultAction.TRY_AGAIN) else listOf(BittensorResultAction.CLOSE)
    }
}
