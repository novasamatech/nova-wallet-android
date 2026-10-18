package io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.valid
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationPayload
import java.math.BigInteger

class BittensorRootStakeHoldValidation : BittensorStakeValidation {

    override suspend fun validate(value: BittensorStakeValidationPayload): ValidationStatus<BittensorStakeValidationFailure> {
        val lastStakeBlock = value.preflight.lastStakeBlock
        if (!value.checkRootHold || lastStakeBlock == null) return valid()
        val elapsedBlocks = (value.preflight.currentBlock - lastStakeBlock).max(BigInteger.ZERO)
        return validOrError(elapsedBlocks >= value.preflight.rootUnlockInterval) { BittensorStakeValidationFailure.ROOT_STAKE_LOCKED }
    }
}
