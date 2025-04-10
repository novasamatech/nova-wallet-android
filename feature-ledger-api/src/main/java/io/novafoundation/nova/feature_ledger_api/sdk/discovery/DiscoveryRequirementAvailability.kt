package io.novafoundation.nova.feature_ledger_api.sdk.discovery

data class DiscoveryRequirementAvailability(
    val satisfiedRequirements: Set<DiscoveryRequirement>,
    val permissionsGranted: Boolean
)

fun DiscoveryRequirementAvailability.grantPermissions(): DiscoveryRequirementAvailability {
    return copy(permissionsGranted = true)
}

fun DiscoveryRequirementAvailability.satisfyRequirement(requirement: DiscoveryRequirement): DiscoveryRequirementAvailability {
    return copy(
        satisfiedRequirements = satisfiedRequirements + requirement
    )
}

fun DiscoveryRequirementAvailability.missRequirement(requirement: DiscoveryRequirement): DiscoveryRequirementAvailability {
    return copy(
        satisfiedRequirements = satisfiedRequirements - requirement
    )
}
