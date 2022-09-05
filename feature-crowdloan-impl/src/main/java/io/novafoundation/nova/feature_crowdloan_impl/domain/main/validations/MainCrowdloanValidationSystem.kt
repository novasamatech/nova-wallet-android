package io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount
import io.novafoundation.nova.feature_crowdloan_impl.domain.main.validations.MainCrowdloanValidationFailure.NoRelaychainAccount

typealias MainCrowdloanValidationSystem = ValidationSystem<MainCrowdloanValidationPayload, MainCrowdloanValidationFailure>

fun ValidationSystem.Companion.mainCrowdloan(): MainCrowdloanValidationSystem = ValidationSystem {
    hasChainAccount(
        chain = MainCrowdloanValidationPayload::chain,
        metaAccount = MainCrowdloanValidationPayload::metaAccount,
        error = ::NoRelaychainAccount
    )
}
