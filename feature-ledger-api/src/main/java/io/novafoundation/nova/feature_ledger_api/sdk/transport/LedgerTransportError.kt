package io.novafoundation.nova.feature_ledger_api.sdk.transport

class LedgerTransportError(val reason: Reason) : Exception(reason.toString()) {

    enum class Reason {
        NO_HEADER_FOUND, UNSUPPORTED_RESPONSE, INCOMPLETE_RESPONSE, NO_MESSAGE_SIZE_FOUND
    }
}
