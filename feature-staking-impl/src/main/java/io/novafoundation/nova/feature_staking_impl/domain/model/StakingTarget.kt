package io.novafoundation.nova.feature_staking_impl.domain.model

import io.novafoundation.nova.feature_staking_api.domain.model.Validator
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.NominationPool

interface StakingTarget {

    class Validators(val validators: List<Validator>) : StakingTarget

    class Pool(val nominationPool: NominationPool) : StakingTarget
}
