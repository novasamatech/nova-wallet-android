package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.common.validation.isFalseOrError
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegationAction
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novasama.substrate_sdk_android.extensions.fromHex

class NoPendingRevokeValidationFactory(
    private val delegatorStateRepository: DelegatorStateRepository,
) {

    fun ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>.noPendingRevoke() {
        validate(NoPendingRevokeValidation(delegatorStateRepository))
    }
}

class NoPendingRevokeValidation(
    private val delegatorStateRepository: DelegatorStateRepository,
) : StartParachainStakingValidation {

    override suspend fun validate(value: StartParachainStakingValidationPayload): ValidationStatus<StartParachainStakingValidationFailure> {
        val hasPendingRevoke = when (val delegatorState = value.delegatorState) {
            is DelegatorState.Delegator -> {
                val collatorId = value.collator.accountIdHex.fromHex()
                val pendingRequest = delegatorStateRepository.scheduledDelegationRequest(delegatorState, collatorId)

                pendingRequest != null && pendingRequest.action is DelegationAction.Revoke
            }
            is DelegatorState.None -> false
        }

        return hasPendingRevoke isFalseOrError { StartParachainStakingValidationFailure.PendingRevoke }
    }
}
