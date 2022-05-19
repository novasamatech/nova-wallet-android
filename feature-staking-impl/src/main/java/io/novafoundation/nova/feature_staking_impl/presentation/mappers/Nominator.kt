package io.novafoundation.nova.feature_staking_impl.presentation.mappers

import io.novafoundation.nova.feature_staking_api.domain.model.IndividualExposure
import io.novafoundation.nova.feature_staking_impl.presentation.validators.parcel.StakerParcelModel

fun mapNominatorToNominatorParcelModel(nominator: IndividualExposure): StakerParcelModel {
    return with(nominator) {
        StakerParcelModel(who, value)
    }
}
