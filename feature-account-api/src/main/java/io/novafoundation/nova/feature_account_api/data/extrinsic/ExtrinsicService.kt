package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface ExtrinsicService {

    suspend fun submitExtrinsicWithSelectedWallet(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Result<*>

    suspend fun submitAndWatchExtrinsicWithSelectedWallet(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Flow<ExtrinsicStatus>

    suspend fun submitExtrinsicWithAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Result<String>

    suspend fun submitAndWatchExtrinsicAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): Flow<ExtrinsicStatus>

    suspend fun paymentInfo(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): FeeResponse

    suspend fun estimateFee(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): BigInteger

    suspend fun estimateFee(chainId: ChainId, extrinsic: String): BigInteger
}
