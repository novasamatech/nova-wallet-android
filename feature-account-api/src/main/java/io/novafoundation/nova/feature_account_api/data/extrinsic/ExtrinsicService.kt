package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

typealias FormExtrinsicWithOrigin = suspend ExtrinsicBuilder.(origin: AccountId) -> Unit

typealias FormMultiExtrinsicWithOrigin = suspend CallBuilder.(origin: AccountId) -> Unit
typealias FormMultiExtrinsic = suspend CallBuilder.() -> Unit

typealias ExtrinsicHash = String

class ExtrinsicSubmission(val hash: String, val origin: AccountId)

interface ExtrinsicService {

    suspend fun submitMultiExtrinsicAwaitingInclusion(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsicWithOrigin,
    ): RetriableMultiResult<ExtrinsicStatus.InBlock>

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use submitExtrinsicWithSelectedWalletV2 instead")
    suspend fun submitExtrinsicWithSelectedWallet(
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<ExtrinsicHash> = submitExtrinsicWithSelectedWalletV2(chain, formExtrinsic)
        .map { it.hash }

    suspend fun submitExtrinsicWithSelectedWalletV2(
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<ExtrinsicSubmission>

    suspend fun submitAndWatchExtrinsicWithSelectedWallet(
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Flow<ExtrinsicStatus>

    suspend fun submitExtrinsicWithAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<ExtrinsicHash>

    suspend fun submitAndWatchExtrinsicAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Flow<ExtrinsicStatus>

    suspend fun paymentInfo(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): FeeResponse

    suspend fun estimateFee(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): BigInteger

    suspend fun estimateFeeV2(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Fee

    suspend fun estimateMultiFee(
        chain: Chain,
        formExtrinsic: FormMultiExtrinsic,
    ): Fee

    suspend fun estimateFee(chain: Chain, extrinsic: String): Fee
}
