package io.novafoundation.nova.feature_ledger_api.sdk.discovery

enum class DiscoveryMethod {
    BLE,
    USB,
    ALL
}

fun DiscoveryMethod.isBluetoothRequired() = this == DiscoveryMethod.BLE

fun DiscoveryMethod.isBluetoothUsing() = this == DiscoveryMethod.BLE || this == DiscoveryMethod.ALL
