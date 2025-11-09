package io.novafoundation.nova.feature_account_api.data.ethereum.transaction

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy

class EthereumTransactionExecution(
    val extrinsicHash: String,
    val blockHash: BlockHash,
    val submissionHierarchy: SubmissionHierarchy
)
