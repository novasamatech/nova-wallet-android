package io.novafoundation.nova.feature_ledger_api.sdk.discovery

class DiscoveryMethods(vararg val methods: Method) {
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

fun DiscoveryMethods.hasRequirement(requirement: DiscoveryRequirement) = discoveryRequirements().contains(requirement)

fun DiscoveryMethods.discoveryRequirements() = methods.flatMap {
    when (it) {
        DiscoveryMethods.Method.BLE -> listOf(DiscoveryRequirement.BLUETOOTH, DiscoveryRequirement.LOCATION)
        DiscoveryMethods.Method.USB -> emptyList()
    }
}

// Requirements are necessary when we use single discovery method and requrements aren't empty
fun DiscoveryMethods.isRequirementsNecessary() = this.methods.size == 1 && discoveryRequirements().isNotEmpty()

fun DiscoveryMethods.isPermissionsRequired() = isRequirementsNecessary()

fun DiscoveryMethods.isBluetoothUsing() = methods.contains(DiscoveryMethods.Method.BLE)
