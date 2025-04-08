package io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.stateMachine

import io.novafoundation.nova.feature_ledger_api.sdk.device.LedgerDevice
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryMethods
import io.novafoundation.nova.feature_ledger_api.sdk.discovery.DiscoveryRequirement

sealed class SideEffect {

    class RequestPermissions(val requirements: List<DiscoveryRequirement>, val shouldExitUponDenial: Boolean) : SideEffect()

    class RequestSatisfyRequirement(val requirements: List<DiscoveryRequirement>) : SideEffect()

    class PresentLedgerFailure(val reason: Throwable, val device: LedgerDevice) : SideEffect()

    class VerifyConnection(val device: LedgerDevice) : SideEffect()

    class StartDiscovery(val method: DiscoveryMethods.Method) : SideEffect()

    class StopDiscovery(val method: DiscoveryMethods.Method) : SideEffect()
}

sealed class SelectLedgerEvent {

    class DiscoveryRequirementSatisfied(val requirement: DiscoveryRequirement) : SelectLedgerEvent()

    class DiscoveryRequirementMissing(val requirement: DiscoveryRequirement) : SelectLedgerEvent()

    object PermissionsGranted : SelectLedgerEvent()

    object AvailabilityRequestsAllowed : SelectLedgerEvent()

    class DiscoveredDevicesListChanged(val newDevices: List<LedgerDevice>) : SelectLedgerEvent()

    class DeviceChosen(val device: LedgerDevice) : SelectLedgerEvent()

    class VerificationFailed(val reason: Throwable) : SelectLedgerEvent()

    object ConnectionVerified : SelectLedgerEvent()
}
