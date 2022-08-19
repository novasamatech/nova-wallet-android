package io.novafoundation.nova.feature_ledger_api.sdk.transport

class LedgerTransportError(val reason: Reason): Exception() {

    enum class Reason {
        DEVICE_NOT_CONNECTED, NO_HEADER_FOUND, UNSUPPORTED_RESPONSE, INCOMPLETE_RESPONSE, NO_MESSAGE_SIZE_FOUND
    }
}
