package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

enum class LedgerApplicationResponse(val code: UShort) {
    UNKNOWN(1u),
    BAD_REQUEST(2u),
    UNSUPPORTED(3u),
    INELIGIBLE_DEVICE(4u),
    TIMEOUT_U2F(5u),
    TIMEOUT(14u),
    NO_ERROR(0x9000u),
    DEVICE_BUSY(0x9001u),
    DERIVING_KEY_ERROR(0x6802u),
    EXECUTION_ERROR(0x6400u),
    WRONG_LENGTH(0x6700u),
    EMPTY_BUFFER(0x6982u),
    OUTPUT_BUFFER_TOO_SMALL(0x6983u),
    INVALID_DATA(0x6984u),
    CONDITIONS_NOT_SATISFIED(0x6985u),
    TRANSACTION_REJECTED(0x6986u),
    BAD_KEY(0x6A80u),
    INVALID_P1P2(0x6B00u),
    INSTRUCTION_NOT_SUPPORTED(0x6D00u),
    WRONG_APP_OPEN(0x6E00u),
    APP_NOT_OPEN(0x6E01u),
    UNKNOWN_ERROR(0x6F00u),
    SIGN_VERIFY_ERROR(0x6F01u);

    companion object {
        fun fromCode(code: UShort): LedgerApplicationResponse {
            return values().firstOrNull { it.code == code } ?: UNKNOWN
        }
    }
}
