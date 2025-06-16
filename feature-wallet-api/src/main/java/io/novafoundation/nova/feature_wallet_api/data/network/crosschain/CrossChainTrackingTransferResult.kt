package io.novafoundation.nova.feature_wallet_api.data.network.crosschain

import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class CrossChainTrackingTransferResult(
    val executionResult: ExtrinsicExecutionResult,
    val balance: Balance
)
