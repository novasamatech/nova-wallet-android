package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

typealias EvmTransactionBuilding = EvmTransactionBuilder.() -> Unit

interface EvmTransactionService {

    suspend fun calculateFee(
        chainId: ChainId,
        building: EvmTransactionBuilding
    ): BigInteger

    suspend fun transact(
        chainId: ChainId,
        origin: TransactionOrigin = TransactionOrigin.SelectedWallet,
        building: EvmTransactionBuilding
    ): Result<TransactionHash>
}


