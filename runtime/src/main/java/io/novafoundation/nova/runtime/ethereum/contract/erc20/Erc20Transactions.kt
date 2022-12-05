package io.novafoundation.nova.runtime.ethereum.contract.erc20

import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

interface Erc20Transactions {

    fun transfer(recipient: AccountId, amount: BigInteger)
}
