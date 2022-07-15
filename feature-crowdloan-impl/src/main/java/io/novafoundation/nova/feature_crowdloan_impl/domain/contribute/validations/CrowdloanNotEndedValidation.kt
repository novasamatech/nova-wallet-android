package io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations

import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_crowdloan_api.data.repository.CrowdloanRepository
import io.novafoundation.nova.runtime.repository.ChainStateRepository

class CrowdloanNotEndedValidation(
    private val chainStateRepository: ChainStateRepository,
    private val crowdloanRepository: CrowdloanRepository
) : ContributeValidation {

    override suspend fun validate(value: ContributeValidationPayload): ValidationStatus<ContributeValidationFailure> {
        val chainId = value.asset.token.configuration.chainId
        val currentBlock = chainStateRepository.currentBlock(chainId)

        val leasePeriodToBlocksConverter = crowdloanRepository.leasePeriodToBlocksConverter(chainId)

        val currentLeaseIndex = leasePeriodToBlocksConverter.leaseIndexFromBlock(currentBlock)

        return when {
            currentBlock >= value.crowdloan.fundInfo.end -> crowdloanEndedFailure()
            currentLeaseIndex > value.crowdloan.fundInfo.firstSlot -> crowdloanEndedFailure()
            else -> ValidationStatus.Valid()
        }
    }

    private fun crowdloanEndedFailure(): ValidationStatus.NotValid<ContributeValidationFailure> =
        ValidationStatus.NotValid(DefaultFailureLevel.ERROR, ContributeValidationFailure.CrowdloanEnded)
}
