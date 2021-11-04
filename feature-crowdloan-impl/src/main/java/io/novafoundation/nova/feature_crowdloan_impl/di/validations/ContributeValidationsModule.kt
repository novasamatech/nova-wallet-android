package io.novafoundation.nova.feature_crowdloan_impl.di.validations

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.CapExceededValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeEnoughToPayFeesValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeExistentialDepositValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.CrowdloanNotEndedValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.DefaultMinContributionValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.PublicCrowdloanValidation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ContributeValidationsModule {

    @Provides
    @IntoSet
    @FeatureScope
    fun provideFeesValidation(): ContributeValidation = ContributeEnoughToPayFeesValidation(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        extraAmountExtractor = { it.contributionAmount },
        errorProducer = { ContributeValidationFailure.CannotPayFees }
    )

    @Provides
    @IntoSet
    @FeatureScope
    fun provideMinContributionValidation(
        crowdloanRepository: CrowdloanRepository,
    ): ContributeValidation = DefaultMinContributionValidation(crowdloanRepository)

    @Provides
    @IntoSet
    @FeatureScope
    fun provideCapExceededValidation(): ContributeValidation = CapExceededValidation()

    @Provides
    @IntoSet
    @FeatureScope
    fun provideCrowdloanNotEndedValidation(
        chainStateRepository: ChainStateRepository,
        crowdloanRepository: CrowdloanRepository,
    ): ContributeValidation = CrowdloanNotEndedValidation(chainStateRepository, crowdloanRepository)

    @Provides
    @IntoSet
    @FeatureScope
    fun provideExistentialWarningValidation(
        walletConstants: WalletConstants,
    ): ContributeValidation = ContributeExistentialDepositValidation(
        walletConstants = walletConstants,
        totalBalanceProducer = { it.asset.total },
        feeProducer = { it.fee },
        extraAmountProducer = { it.contributionAmount },
        tokenProducer = { it.asset.token },
        errorProducer = { ContributeValidationFailure.ExistentialDepositCrossed },
    )

    @Provides
    @IntoSet
    @FeatureScope
    fun providePublicCrowdloanValidation(
        customContributeManager: CustomContributeManager,
    ): ContributeValidation = PublicCrowdloanValidation(customContributeManager)
}
