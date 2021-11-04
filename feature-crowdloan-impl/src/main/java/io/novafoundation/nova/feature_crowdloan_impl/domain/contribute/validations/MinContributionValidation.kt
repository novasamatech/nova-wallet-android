package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.validOrError
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import java.math.BigInteger

class DefaultMinContributionValidation(
    private val crowdloanRepository: CrowdloanRepository,
) : MinContributionValidation() {

    override suspend fun minContribution(payload: ContributeValidationPayload): BigInteger {
        val chainAsset = payload.asset.token.configuration

        return crowdloanRepository.minContribution(chainAsset.chainId)
    }
}

abstract class MinContributionValidation : ContributeValidation {

    abstract suspend fun minContribution(payload: ContributeValidationPayload): BigInteger

    override suspend fun validate(value: ContributeValidationPayload): ValidationStatus<ContributeValidationFailure> {
        val chainAsset = value.asset.token.configuration

        val minContributionInPlanks = minContribution(value)
        val minContribution = chainAsset.amountFromPlanks(minContributionInPlanks)

        return validOrError(value.contributionAmount >= minContribution) {
            ContributeValidationFailure.LessThanMinContribution(minContribution, chainAsset)
        }
    }
}
