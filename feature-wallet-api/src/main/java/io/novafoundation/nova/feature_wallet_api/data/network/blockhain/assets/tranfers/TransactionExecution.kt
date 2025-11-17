package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.tranfers

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EthereumTransactionExecution
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult

sealed interface TransactionExecution {

    class Ethereum(val ethereumTransactionExecution: EthereumTransactionExecution) : TransactionExecution

    class Substrate(val extrinsicExecutionResult: ExtrinsicExecutionResult) : TransactionExecution
}
