package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import android.os.Parcelable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootStakeRequest
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.parcelize.Parcelize

@Parcelize
class BittensorStakeConfirmPayload(
    val metaId: Long,
    val amountInPlanks: BigInteger,
    val validatorAddress: String,
    val validatorName: String,
    val existingPosition: Boolean = false
) : Parcelable

fun BittensorStakeConfirmPayload.toStakeRequest(chain: Chain): BittensorRootStakeRequest {
    val validator = BittensorValidator(AccountIdKey(chain.accountIdOf(validatorAddress)), validatorName)
    return BittensorRootStakeRequest(metaId, chain, validator, amountInPlanks)
}
