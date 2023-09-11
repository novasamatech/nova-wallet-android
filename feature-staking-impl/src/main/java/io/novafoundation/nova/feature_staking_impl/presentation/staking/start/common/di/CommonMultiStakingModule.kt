package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.RealStartMultiStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StartMultiStakingInteractor

@Module
class CommonMultiStakingModule {

    @Provides
    @ScreenScope
    fun provideInteractor(
        extrinsicService: ExtrinsicService,
        accountRepository: AccountRepository,
    ): StartMultiStakingInteractor = RealStartMultiStakingInteractor(extrinsicService, accountRepository)
}
