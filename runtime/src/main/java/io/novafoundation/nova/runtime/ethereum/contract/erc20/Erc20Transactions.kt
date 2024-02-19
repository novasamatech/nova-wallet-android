package io.novafoundation.nova.runtime.ethereum.contract.erc20

import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

interface Erc20Transactions {

    fun transfer(recipient: AccountId, amount: BigInteger)
}
