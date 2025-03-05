package io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.model

import io.novafoundation.nova.feature_staking_impl.domain.common.singleSelect.model.TargetWithStakedAmount
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosCollator
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.StakeTargetModel

typealias MythosSelectCollatorModel = SelectStakeTargetModel<MythosCollator>
typealias MythosCollatorModel = StakeTargetModel<MythosCollator>
typealias MythosCollatorWithAmount = TargetWithStakedAmount<MythosCollator>
