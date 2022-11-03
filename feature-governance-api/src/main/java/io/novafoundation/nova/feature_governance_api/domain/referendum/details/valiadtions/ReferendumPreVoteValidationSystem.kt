package io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.validation.hasChainAccount
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.valiadtions.ReferendumPreVoteValidationFailure.NoRelaychainAccount

typealias ReferendumPreVoteValidationSystem = ValidationSystem<ReferendumPreVoteValidationPayload, ReferendumPreVoteValidationFailure>

fun ValidationSystem.Companion.referendumPreVote(): ReferendumPreVoteValidationSystem = ValidationSystem {
    hasChainAccount(
        chain = ReferendumPreVoteValidationPayload::chain,
        metaAccount = ReferendumPreVoteValidationPayload::metaAccount,
        error = ::NoRelaychainAccount
    )
}
