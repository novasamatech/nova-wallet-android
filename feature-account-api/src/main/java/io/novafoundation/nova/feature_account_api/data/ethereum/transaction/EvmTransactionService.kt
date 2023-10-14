package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.ethereum.transaction.builder.EvmTransactionBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger

typealias EvmTransactionBuilding = EvmTransactionBuilder.() -> Unit

interface EvmTransactionService {

    suspend fun calculateFee(
        chainId: ChainId,
        origin: TransactionOrigin = TransactionOrigin.SelectedWallet,
        fallbackGasLimit: BigInteger = DefaultGasProvider.GAS_LIMIT,
        building: EvmTransactionBuilding,
    ): Fee

    suspend fun transact(
        chainId: ChainId,
        presetFee: Fee?,
        origin: TransactionOrigin = TransactionOrigin.SelectedWallet,
        fallbackGasLimit: BigInteger = DefaultGasProvider.GAS_LIMIT,
        building: EvmTransactionBuilding,
    ): Result<TransactionHash>
}
