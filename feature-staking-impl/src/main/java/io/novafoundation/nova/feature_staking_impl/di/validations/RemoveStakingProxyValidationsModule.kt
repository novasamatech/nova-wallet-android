package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.sufficientBalanceToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.sufficientBalanceToStayAboveEd
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.RemoveStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.sufficientBalanceToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.remove.sufficientBalanceToStayAboveEd
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

@Module
class RemoveStakingProxyValidationsModule {

    @FeatureScope
    @Provides
    fun provideAddStakingProxyValidationSystem(
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): RemoveStakingProxyValidationSystem = ValidationSystem {
        sufficientBalanceToPayFee()

        sufficientBalanceToStayAboveEd(enoughTotalToStayAboveEDValidationFactory)
    }
}
