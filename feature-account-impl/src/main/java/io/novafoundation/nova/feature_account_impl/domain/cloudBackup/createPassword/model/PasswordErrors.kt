package io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.model

enum class PasswordErrors {
    TOO_SHORT,
    NO_LETTERS,
    NO_DIGITS,
    PASSWORDS_DO_NOT_MATCH
}
