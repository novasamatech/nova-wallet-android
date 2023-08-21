package io.novafoundation.nova.feature_staking_impl.domain.model

import io.novafoundation.nova.feature_staking_api.domain.model.Validator

interface StakingTarget {

    class Validators(val validators: List<Validator>) : StakingTarget

    class NominationPool : StakingTarget
}
