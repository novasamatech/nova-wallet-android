package io.novafoundation.nova.feature_ledger_api.sdk.discovery

data class DiscoveryRequirementAvailability(
    val satisfiedRequirements: Set<DiscoveryRequirement>,
    val permissionsGranted: Boolean
)

fun DiscoveryRequirementAvailability.permissionGranted(): DiscoveryRequirementAvailability {
    return copy(permissionsGranted = true)
}

fun DiscoveryRequirementAvailability.requirementSatisfied(requirement: DiscoveryRequirement): DiscoveryRequirementAvailability {
    return copy(
        satisfiedRequirements = satisfiedRequirements + requirement
    )
}

fun DiscoveryRequirementAvailability.requirementMissing(requirement: DiscoveryRequirement): DiscoveryRequirementAvailability {
    return copy(
        satisfiedRequirements = satisfiedRequirements - requirement
    )
}
