package io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.Identifiable

class SelectStakeTargetModel<out T : Identifiable>(
    val addressModel: AddressModel,
    val subtitle: CharSequence?,
    val active: Boolean,
    val payload: T,
) : Identifiable by payload
