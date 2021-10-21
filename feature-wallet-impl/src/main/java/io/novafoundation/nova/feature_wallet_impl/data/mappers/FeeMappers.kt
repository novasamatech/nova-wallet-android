package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.feature_wallet_api.domain.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Transfer
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import java.math.BigInteger

fun mapFeeRemoteToFee(fee: BigInteger, transfer: Transfer): Fee {
    return with(transfer) {
        Fee(
            transferAmount = amount,
            feeAmount = chainAsset.amountFromPlanks(fee),
            type = chainAsset
        )
    }
}
