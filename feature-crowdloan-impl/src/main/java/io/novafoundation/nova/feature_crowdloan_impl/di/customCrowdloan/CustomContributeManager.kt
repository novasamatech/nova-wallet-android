package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan

import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.CustomContributeSubmitter

class CustomContributeManager(
    private val factories: Set<CustomContributeFactory>
) {

    fun isCustomFlowSupported(flowType: String): Boolean {
        return relevantFactoryOrNull(flowType) != null
    }

    fun getSubmitter(flowType: String): CustomContributeSubmitter {
        return relevantFactory(flowType).submitter
    }

    fun getFactoryOrNull(flowType: String): CustomContributeFactory? = relevantFactoryOrNull(flowType)

    private fun relevantFactory(flowType: String) = relevantFactoryOrNull(flowType) ?: noFactoryFound(flowType)

    private fun relevantFactoryOrNull(
        flowType: String,
    ): CustomContributeFactory? {
        return factories.firstOrNull { it.supports(flowType) }
    }

    fun relevantExtraBonusFlow(flowType: String): ExtraBonusFlow {
        val factory = relevantFactory(flowType)

        return factory.extraBonusFlow ?: unexpectedBonusFlow(flowType)
    }

    private fun noFactoryFound(flowType: String): Nothing = throw NoSuchElementException("Factory for $flowType was not found")

    private fun unexpectedBonusFlow(flowType: String): Nothing = throw IllegalStateException("No extra bonus flow found for flow $flowType")
}
