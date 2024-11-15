package io.novafoundation.nova.feature_crowdloan_impl.di.validations

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeManager
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.BonusAppliedValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.CapExceededValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeEnoughToPayFeesValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeExistentialDepositValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.CrowdloanNotEndedValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.DefaultMinContributionValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.PublicCrowdloanValidation
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletConstants
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.balanceCountedTowardsED
import io.novafoundation.nova.feature_wallet_api.domain.validation.EnoughAmountToTransferValidationGeneric
import io.novafoundation.nova.runtime.repository.ChainStateRepository

@Module
class ContributeValidationsModule {

    @Provides
    @FeatureScope
    fun provideFeesValidation(): ContributeEnoughToPayFeesValidation = EnoughAmountToTransferValidationGeneric(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        extraAmountExtractor = { it.contributionAmount },
        errorProducer = { ContributeValidationFailure.CannotPayFees }
    )

    @Provides
    @FeatureScope
    fun provideMinContributionValidation(
        crowdloanRepository: CrowdloanRepository,
    ) = DefaultMinContributionValidation(crowdloanRepository)

    @Provides
    @FeatureScope
    fun provideCapExceededValidation() = CapExceededValidation()

    @Provides
    @FeatureScope
    fun provideCrowdloanNotEndedValidation(
        chainStateRepository: ChainStateRepository,
        crowdloanRepository: CrowdloanRepository,
    ) = CrowdloanNotEndedValidation(chainStateRepository, crowdloanRepository)

    @Provides
    @FeatureScope
    fun provideExistentialWarningValidation(
        walletConstants: WalletConstants,
    ) = ContributeExistentialDepositValidation(
        countableTowardsEdBalance = { it.asset.balanceCountedTowardsED() },
        feeProducer = { listOf(it.fee) },
        extraAmountProducer = { it.contributionAmount },
        existentialDeposit = {
            val inPlanks = walletConstants.existentialDeposit(it.asset.token.configuration.chainId)

            it.asset.token.amountFromPlanks(inPlanks)
        },
        errorProducer = { _, _ -> ContributeValidationFailure.ExistentialDepositCrossed },
    )

    @Provides
    @FeatureScope
    fun providePublicCrowdloanValidation(
        customContributeManager: CustomContributeManager,
    ) = PublicCrowdloanValidation(customContributeManager)

    @Provides
    @FeatureScope
    fun provideBonusAppliedValidation(
        customContributeManager: CustomContributeManager,
    ) = BonusAppliedValidation(customContributeManager)

    @Provides
    @Select
    @FeatureScope
    fun provideSelectContributeValidationSet(
        feesValidation: ContributeEnoughToPayFeesValidation,
        minContributionValidation: DefaultMinContributionValidation,
        capExceededValidation: CapExceededValidation,
        crowdloanNotEndedValidation: CrowdloanNotEndedValidation,
        contributeExistentialDepositValidation: ContributeExistentialDepositValidation,
        publicCrowdloanValidation: PublicCrowdloanValidation,
        bonusAppliedValidation: BonusAppliedValidation,
    ): Set<ContributeValidation> = setOf(
        feesValidation,
        minContributionValidation,
        capExceededValidation,
        crowdloanNotEndedValidation,
        contributeExistentialDepositValidation,
        publicCrowdloanValidation,
        bonusAppliedValidation
    )

    @Provides
    @Confirm
    @FeatureScope
    fun provideConfirmContributeValidationSet(
        feesValidation: ContributeEnoughToPayFeesValidation,
        minContributionValidation: DefaultMinContributionValidation,
        capExceededValidation: CapExceededValidation,
        crowdloanNotEndedValidation: CrowdloanNotEndedValidation,
        contributeExistentialDepositValidation: ContributeExistentialDepositValidation,
        publicCrowdloanValidation: PublicCrowdloanValidation,
    ): Set<ContributeValidation> = setOf(
        feesValidation,
        minContributionValidation,
        capExceededValidation,
        crowdloanNotEndedValidation,
        contributeExistentialDepositValidation,
        publicCrowdloanValidation,
    )
}
