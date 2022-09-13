package io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.base

import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.ContributeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.MinContributionValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.acala.AcalaMinContributionValidation
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.ContributeCustomization

abstract class AcalaMainFlowCustomization<V> : ContributeCustomization<V> {

    override fun modifyValidations(validations: Collection<ContributeValidation>): Collection<ContributeValidation> {
        return validations.map {
            when (it) {
                is MinContributionValidation -> AcalaMinContributionValidation(fallback = it)
                else -> it
            }
        }
    }
}
