package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount

typealias StartStakingLandingValidationSystem = ValidationSystem<StartStakingLandingValidationPayload, StartStakingLandingValidationFailure>

fun ValidationSystem.Companion.startStakingLanding(): StartStakingLandingValidationSystem = ValidationSystem {
    hasChainAccount(
        chain = { it.chain },
        metaAccount = { it.metaAccount },
        error = StartStakingLandingValidationFailure::NoChainAccountFound
    )
}
