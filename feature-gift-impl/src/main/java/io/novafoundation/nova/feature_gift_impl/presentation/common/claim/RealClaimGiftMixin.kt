package io.novafoundation.nova.feature_gift_impl.presentation.common.claim

import io.novafoundation.nova.common.utils.mapFailure
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_gift_impl.domain.ClaimGiftInteractor
import io.novafoundation.nova.feature_gift_impl.domain.models.ClaimableGift
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftAmountWithFee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class ClaimGiftMixinFactory(private val claimGiftInteractor: ClaimGiftInteractor) {

    fun create(
        coroutineScope: CoroutineScope
    ): ClaimGiftMixin {
        return RealClaimGiftMixin(
            claimGiftInteractor,
            coroutineScope
        )
    }
}

class RealClaimGiftMixin(
    private val claimGiftInteractor: ClaimGiftInteractor,
    private val coroutineScope: CoroutineScope,
) : ClaimGiftMixin {

    override val claimingInProgressFlow = MutableStateFlow(false)

    override suspend fun claimGift(
        gift: ClaimableGift,
        amountWithFee: GiftAmountWithFee,
        giftMetaAccount: MetaAccount,
        giftRecipient: MetaAccount
    ): Result<Unit> {
        claimingInProgressFlow.value = true

        if (claimGiftInteractor.isGiftAlreadyClaimed(gift)) {
            claimingInProgressFlow.value = false

            return Result.failure(ClaimGiftException.GiftAlreadyClaimed())
        }

        return claimGiftInteractor.claimGift(
            claimableGift = gift,
            giftAmountWithFee = amountWithFee,
            giftMetaAccount = giftMetaAccount,
            giftRecipient = giftRecipient,
            coroutineScope = coroutineScope
        )
            .mapFailure {
                claimingInProgressFlow.value = false

                ClaimGiftException.UnknownError(it)
            }
    }
}
