package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.enoughBalanceToPayDeposit
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.maximumProxies
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.sufficientBalanceToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.sufficientBalanceToStayAboveEd
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.validAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

@Module
class AddStakingProxyValidationsModule {

    @FeatureScope
    @Provides
    fun provideAddStakingProxyValidationSystem(
        getProxyRepository: GetProxyRepository,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): AddStakingProxyValidationSystem = ValidationSystem {
        validAddress()

        sufficientBalanceToPayFee()

        sufficientBalanceToStayAboveEd(enoughTotalToStayAboveEDValidationFactory)

        maximumProxies(getProxyRepository)

        enoughBalanceToPayDeposit(getProxyRepository)
    }
}
