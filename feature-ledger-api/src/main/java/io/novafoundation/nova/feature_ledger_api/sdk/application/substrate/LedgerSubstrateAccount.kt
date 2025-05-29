package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.extensions.asEthereumAccountId
import io.novasama.substrate_sdk_android.extensions.toAddress
import io.novasama.substrate_sdk_android.runtime.AccountId

class LedgerSubstrateAccount(
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
)

// Ledger EVM shares derivation path with Ledger Substrate
class LedgerEvmAccount(
    val accountId: AccountId,
    val publicKey: ByteArray,
)

fun LedgerEvmAccount.address(): String {
    return accountId.asEthereumAccountId().toAddress().value
}
