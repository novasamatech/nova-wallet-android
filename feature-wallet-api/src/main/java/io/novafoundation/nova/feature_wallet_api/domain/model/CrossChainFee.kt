package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sum
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFeeBase
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.network.crosschain.CrossChainFeeMetadata
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CrossChainTransferFee(
    /**
     * Deducted upon initial transaction submission from the origin chain. Asset can be controlled with [FeePaymentCurrency]
     */
    val submissionFee: SubmissionFee,

    /**
     * Fees paid initial submission of the tx, this consists of variety of xcm-related fees
     */
    val postSubmissionFee: PostSubmissionFee,
) {

    class PostSubmissionFee(
        /**
         * Deducted upon initial transaction submission from the origin chain. Cannot be controlled with [FeePaymentCurrency]
         * and is always paid in native currency
         * Example: Delivery Fee when JIT Withdraw is enabled
         */
        val byAccount: SubmissionFee?,
        /**
         *  Total sum of all execution and delivery fees paid from holding register throughout xcm transfer
         *  Paid (at the moment) in a sending asset
         *
         *  Example: Execution fee, Delivery Fee when JIT Withdraw is disabled
         */
        val fromAmount: PostSubmissionAmountFee
    )

    class PostSubmissionAmountFee(
        val originAsset: Chain.Asset,
        private val feesByChain: Map<ChainId, Balance>,
        val metadata: CrossChainFeeMetadata,
    ) {

        val totalFee: FeeBase = SubstrateFeeBase(feesByChain.values.sum(), originAsset)

        fun getFeeOn(chainId: ChainId): Balance {
            return feesByChain[chainId].orZero()
        }
    }
}
