package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.runtime.ethereum.EvmRpcException
import io.novafoundation.nova.runtime.extrinsic.DryRunRpcException
import io.novasama.substrate_sdk_android.wsrpc.exception.ConnectionClosedException
import io.novasama.substrate_sdk_android.wsrpc.exception.RpcException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.IOException
import java.net.SocketTimeoutException

class FeeLoadingErrorTest {

    private val resourceManager = mock(ResourceManager::class.java).apply {
        `when`(getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(getString(R.string.network_fee_error_message)).thenReturn("Could not estimate fee")
        `when`(getString(R.string.choose_amount_network_error)).thenReturn("Network not responding")
        `when`(getString(R.string.choose_amount_error_fee)).thenReturn("Connection failed")
    }

    @Test
    fun `chain response is shown instead of network unavailable`() {
        val error = RpcException("Unable to pay some fees")
        assertEquals("Error" to "Unable to pay some fees", formatFeeLoadingError(error, resourceManager))
    }

    @Test
    fun `wrapped chain error takes precedence over transport wrapper`() {
        val error = IOException("Request failed", RpcException("AssetConversion.AmountOutTooLow"))
        assertEquals("Error" to "AssetConversion.AmountOutTooLow", formatFeeLoadingError(error, resourceManager))
    }

    @Test
    fun `dispatch evm and dry run reasons are preserved`() {
        val errors = listOf(
            DispatchError.Token,
            EvmRpcException(-32000, "execution reverted"),
            DryRunRpcException("system_dryRun: RPC -32601: Method not found"),
        )
        errors.forEach { error ->
            assertEquals("Error" to error.message, formatFeeLoadingError(error, resourceManager))
        }
    }

    @Test
    fun `unavailable connection keeps connection error`() {
        listOf(SocketTimeoutException(), ConnectionClosedException()).forEach { error ->
            assertEquals("Network not responding" to "Connection failed", formatFeeLoadingError(error, resourceManager))
        }
    }

    @Test
    fun `local failure is not reported as unavailable network`() {
        val error = IllegalStateException("Asset Hub commission does not fit the updated swap amount")
        assertEquals("Error" to "Could not estimate fee", formatFeeLoadingError(error, resourceManager))
    }

    @Test
    fun `missing chain reason has neutral fallback`() {
        listOf(RpcException(null), RpcException(" "), DispatchError.Unknown).forEach { error ->
            assertEquals("Error" to "Could not estimate fee", formatFeeLoadingError(error, resourceManager))
        }
    }
}
