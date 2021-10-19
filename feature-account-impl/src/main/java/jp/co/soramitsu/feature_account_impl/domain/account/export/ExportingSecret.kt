package jp.co.soramitsu.feature_account_impl.domain.account.export

class ExportingSecret<T>(
    val derivationPath: String?,
    val secret: T
)
