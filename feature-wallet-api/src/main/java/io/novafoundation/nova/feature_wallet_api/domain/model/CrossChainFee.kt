package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class CrossChainTransferFee(
    /**
     * Deducted upon initial transaction submission from the origin chain. Asset can be controlled with [FeePaymentCurrency]
     */
    val fromOriginInFeeCurrency: SubmissionFee,

    /**
     * Deducted upon initial transaction submission from the origin chain, e.g. to pay for delivery fees. Cannot be controlled with [FeePaymentCurrency]
     * and is always paid in native currency
     *
     */
    val fromOriginInNativeCurrency: FeeBase?,

    /**
     *  Total sum of all execution and delivery fees paid from holding register throughout xcm transfer
     *  Paid (at the moment) in a sending asset. There might be multiple [Chain.Asset] that represent the same logical asset,
     *  the asset here indicates the first one, on the origin chain
     */
    val fromHoldingRegister: FeeBase,
)
