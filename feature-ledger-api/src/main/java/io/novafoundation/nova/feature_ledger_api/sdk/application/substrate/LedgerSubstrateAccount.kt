package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.runtime.AccountId

class LedgerSubstrateAccount(
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
)

class LedgerEvmAccount(
    val accountId: AccountId,
    val publicKey: ByteArray,
    val derivationPath: String
)
