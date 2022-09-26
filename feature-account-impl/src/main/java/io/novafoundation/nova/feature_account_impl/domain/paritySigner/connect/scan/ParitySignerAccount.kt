package io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan

import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.AnySubstrateChainAddressFormat
import io.novafoundation.nova.common.address.format.EthereumAddressFormat
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ParitySignerAccount(
    val accountId: AccountId,
    val accountType: Type,
) {

    enum class Type {
        SUBSTRATE, ETHEREUM
    }
}

val ParitySignerAccount.Type.addressFormat: AddressFormat
    get() = when (this) {
        ParitySignerAccount.Type.SUBSTRATE -> AnySubstrateChainAddressFormat()
        ParitySignerAccount.Type.ETHEREUM -> EthereumAddressFormat()
    }
