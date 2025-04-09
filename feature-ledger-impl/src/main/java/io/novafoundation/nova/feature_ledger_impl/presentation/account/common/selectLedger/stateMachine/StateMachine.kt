package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement

sealed class SideEffect {

    data class RequestPermissions(val requirements: List<DiscoveryRequirement>, val shouldExitUponDenial: Boolean) : SideEffect()

    data class RequestSatisfyRequirement(val requirements: List<DiscoveryRequirement>) : SideEffect()

    data class PresentLedgerFailure(val reason: Throwable, val device: LedgerDevice) : SideEffect()

    data class VerifyConnection(val device: LedgerDevice) : SideEffect()

    data class StartDiscovery(val method: DiscoveryMethods.Method) : SideEffect()

    data class StopDiscovery(val method: DiscoveryMethods.Method) : SideEffect()
}

sealed class SelectLedgerEvent {

    data class DiscoveryRequirementSatisfied(val requirement: DiscoveryRequirement) : SelectLedgerEvent()

    data class DiscoveryRequirementMissing(val requirement: DiscoveryRequirement) : SelectLedgerEvent()

    object PermissionsGranted : SelectLedgerEvent() {
        override fun toString(): String {
            return "PermissionsGranted"
        }
    }

    object AvailabilityRequestsAllowed : SelectLedgerEvent() {
        override fun toString(): String {
            return "AvailabilityRequestsAllowed"
        }
    }

    data class DiscoveredDevicesListChanged(val newDevices: List<LedgerDevice>) : SelectLedgerEvent()

    data class DeviceChosen(val device: LedgerDevice) : SelectLedgerEvent()

    data class VerificationFailed(val reason: Throwable) : SelectLedgerEvent()

    object ConnectionVerified : SelectLedgerEvent() {
        override fun toString(): String {
            return "ConnectionVerified"
        }
    }
}
