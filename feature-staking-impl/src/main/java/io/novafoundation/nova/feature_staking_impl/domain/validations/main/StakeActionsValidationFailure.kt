package io.novafoundation.nova.feature_staking_impl.domain.validations.main

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount

sealed class StakeActionsValidationFailure {

    class UnbondingRequestLimitReached(val limit: Int) : StakeActionsValidationFailure()

    class ControllerRequired(val controllerAddress: String) : StakeActionsValidationFailure()

    class StashRequired(val stashAddress: String) : StakeActionsValidationFailure()

    class StashRequiredToManageProxies(val stashAddress: String, val stashMetaAccount: MetaAccount?) : StakeActionsValidationFailure()
}
