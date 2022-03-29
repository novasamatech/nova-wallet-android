package io.novafoundation.nova.feature_staking_impl.presentation.validators.details.model

import io.novafoundation.nova.common.address.AddressModel

class ValidatorDetailsModel(
    val stake: ValidatorStakeModel,
    val addressModel: AddressModel,
    val identity: IdentityModel?,
)
