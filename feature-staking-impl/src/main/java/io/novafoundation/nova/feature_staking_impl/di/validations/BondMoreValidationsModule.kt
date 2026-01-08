package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.BondMoreValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.bond.bondMore
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

@Module
class BondMoreValidationsModule {

    @Provides
    @FeatureScope
    fun provideBondMoreValidationSystem(
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): BondMoreValidationSystem = ValidationSystem.bondMore(enoughTotalToStayAboveEDValidationFactory)
}
