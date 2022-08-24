package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

enum class LedgerApplicationResponse(val code: UShort) {
    unknown(1u),
    badRequest(2u),
    unsupported(3u),
    ineligibleDevice(4u),
    timeoutU2f(5u),
    timeout(14u),
    noError(0x9000u),
    deviceBusy(0x9001u),
    derivingKeyError(0x6802u),
    executionError(0x6400u),
    wrongLength(0x6700u),
    emptyBuffer(0x6982u),
    outputBufferTooSmall(0x6983u),
    invalidData(0x6984u),
    conditionsNotSatisfied(0x6985u),
    transactionRejected(0x6986u),
    badKey(0x6A80u),
    invalidP1P2(0x6B00u),
    instructionNotSupported(0x6D00u),
    wrongAppOpen(0x6E00u),
    appNotOpen(0x6E01u),
    unknownError(0x6F00u),
    signVerifyError(0x6F01u);

    companion object {
        fun fromCode(code: UShort): LedgerApplicationResponse {
            return values().firstOrNull { it.code == code } ?: unknown
        }
    }
}
