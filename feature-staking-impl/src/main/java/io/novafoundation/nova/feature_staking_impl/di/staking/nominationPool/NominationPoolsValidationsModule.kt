package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.validations.PoolStateValidationFactory

@Module
class NominationPoolsValidationsModule {

    @Provides
    @FeatureScope
    fun providePoolStateValidationFactory(
        nominationPoolStateRepository: NominationPoolStateRepository
    ) = PoolStateValidationFactory(nominationPoolStateRepository)
}
