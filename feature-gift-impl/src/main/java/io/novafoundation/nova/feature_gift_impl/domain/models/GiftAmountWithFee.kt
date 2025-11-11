package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import java.math.BigDecimal

class GiftAmountWithFee(
    val amount: BigDecimal,
    val fee: SubmissionFee
)
