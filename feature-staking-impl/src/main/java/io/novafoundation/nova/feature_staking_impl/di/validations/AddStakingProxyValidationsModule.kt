package io.novafoundation.nova.feature_staking_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.AddStakingProxyValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.enoughBalanceToPayDepositAndFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.maximumProxies
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.notSelfAccount
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.stakingTypeIsNotDuplication
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.sufficientBalanceToPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.sufficientBalanceToStayAboveEd
import io.novafoundation.nova.feature_staking_impl.domain.validations.delegation.proxy.add.validAddress
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughTotalToStayAboveEDValidationFactory

@Module
class AddStakingProxyValidationsModule {

    @FeatureScope
    @Provides
    fun provideAddStakingProxyValidationSystem(
        getProxyRepository: GetProxyRepository,
        accountRepository: AccountRepository,
        enoughTotalToStayAboveEDValidationFactory: EnoughTotalToStayAboveEDValidationFactory
    ): AddStakingProxyValidationSystem = ValidationSystem {
        validAddress()

        notSelfAccount(accountRepository)

        sufficientBalanceToPayFee()

        sufficientBalanceToStayAboveEd(enoughTotalToStayAboveEDValidationFactory)

        stakingTypeIsNotDuplication(getProxyRepository)

        maximumProxies(getProxyRepository)

        enoughBalanceToPayDepositAndFee()
    }
}
