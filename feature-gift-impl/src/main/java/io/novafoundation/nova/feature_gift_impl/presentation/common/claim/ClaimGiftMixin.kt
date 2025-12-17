package io.novafoundation.nova.feature_gift_impl.presentation.common.claim

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_gift_impl.domain.models.ClaimableGift
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftAmountWithFee
import kotlinx.coroutines.flow.MutableStateFlow

interface ClaimGiftMixin {

    val claimingInProgressFlow: MutableStateFlow<Boolean>

    suspend fun claimGift(
        gift: ClaimableGift,
        amountWithFee: GiftAmountWithFee,
        giftMetaAccount: MetaAccount,
        giftRecipient: MetaAccount
    ): Result<Unit>
}

sealed class ClaimGiftException : Exception() {
    class GiftAlreadyClaimed : ClaimGiftException()

    class UnknownError(throwable: Throwable) : ClaimGiftException()
}
