package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount

typealias StartStakingLandingValidationSystem = ValidationSystem<StartSwipeGovValidationPayload, StartSwipeGovValidationFailure>

fun ValidationSystem.Companion.startSwipeGovValidation(): StartStakingLandingValidationSystem = ValidationSystem {
    hasChainAccount(
        chain = { it.chain },
        metaAccount = { it.metaAccount },
        error = StartSwipeGovValidationFailure::NoChainAccountFound
    )
}
