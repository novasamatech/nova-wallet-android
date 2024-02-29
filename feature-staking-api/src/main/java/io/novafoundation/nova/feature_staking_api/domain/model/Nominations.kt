package io.novafoundation.nova.feature_staking_api.domain.model

import io.novasama.substrate_sdk_android.runtime.AccountId

class Nominations(
    val targets: List<AccountId>,
    val submittedInEra: EraIndex,
    val suppressed: Boolean
)
