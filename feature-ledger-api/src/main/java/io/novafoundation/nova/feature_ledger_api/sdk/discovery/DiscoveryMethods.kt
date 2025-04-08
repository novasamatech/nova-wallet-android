package io.novafoundation.nova.feature_ledger_api.sdk.discovery

import io.novafoundation.nova.common.utils.filterToSet

@JvmInline
value class DiscoveryMethods(val methods: List<Method>) {

    constructor(vararg methods: Method) : this(methods.toList())

    enum class Method {
        BLE,
        USB
    }

    companion object {
        fun all() = DiscoveryMethods(Method.BLE, Method.USB)
    }
}

enum class DiscoveryRequirement {
    BLUETOOTH, LOCATION
}

fun DiscoveryMethods.filterBySatisfiedRequirements(
    discoveryRequirementAvailability: DiscoveryRequirementAvailability
): Set<DiscoveryMethods.Method> {
    return methods.filterToSet { method ->
        val methodRequirements = method.discoveryRequirements()
        val requirementsSatisfied = methodRequirements.all { it in discoveryRequirementAvailability.satisfiedRequirements }
        val availableWithPermissions = methodRequirements.availableWithPermissions(discoveryRequirementAvailability.permissionsGranted)

        requirementsSatisfied && availableWithPermissions
    }
}

private fun List<DiscoveryRequirement>.availableWithPermissions(permissionsGranted: Boolean): Boolean {
    return if (isEmpty()) {
        true
    } else {
        permissionsGranted
    }
}

fun DiscoveryMethods.discoveryRequirements() = methods.flatMap {
    when (it) {
        DiscoveryMethods.Method.BLE -> listOf(DiscoveryRequirement.BLUETOOTH, DiscoveryRequirement.LOCATION)
        DiscoveryMethods.Method.USB -> emptyList()
    }
}

fun DiscoveryMethods.Method.discoveryRequirements(): List<DiscoveryRequirement> {
    return when (this) {
        DiscoveryMethods.Method.BLE -> listOf(DiscoveryRequirement.BLUETOOTH, DiscoveryRequirement.LOCATION)
        DiscoveryMethods.Method.USB -> emptyList()
    }
}
