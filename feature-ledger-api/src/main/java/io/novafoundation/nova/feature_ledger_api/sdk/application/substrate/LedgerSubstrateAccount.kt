package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import jp.co.soramitsu.fearless_utils.encrypt.EncryptionType

class LedgerSubstrateAccount(
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
)
