package io.novafoundation.nova.runtime.ethereum.contract.erc20

import java.math.BigInteger

interface Erc20Transactions {

    fun transfer(recipient: String, amount: BigInteger)
}
