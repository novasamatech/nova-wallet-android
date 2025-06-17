package io.novafoundation.nova.feature_account_api.data.multisig.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.utils.times
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface MultisigValidationsRepository {

    suspend fun getMultisigDepositBase(chainId: ChainId): BalanceOf

    suspend fun getMultisigDepositFactor(chainId: ChainId): BalanceOf

    suspend fun hasPendingCallHash(chainId: ChainId, accountIdKey: AccountIdKey, callHash: CallHash): Boolean
}

suspend fun MultisigValidationsRepository.getMultisigDeposit(chainId: ChainId, threshold: Int): BalanceOf {
    val base = getMultisigDepositBase(chainId)
    val factor = getMultisigDepositFactor(chainId)

    return base + factor * threshold
}
