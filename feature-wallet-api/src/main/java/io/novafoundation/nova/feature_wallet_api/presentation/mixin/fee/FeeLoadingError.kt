package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.base.errors.NovaException
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.runtime.ethereum.EvmRpcException
import io.novafoundation.nova.runtime.extrinsic.DryRunRpcException
import io.novasama.substrate_sdk_android.wsrpc.exception.ConnectionClosedException
import io.novasama.substrate_sdk_android.wsrpc.exception.RpcException
import java.io.IOException

internal fun formatFeeLoadingError(error: Throwable, resourceManager: ResourceManager): TitleAndMessage {
    val causes = generateSequence(error) { it.cause }.toList()
    val chainError = causes.firstOrNull {
        it is RpcException || it is DispatchError || it is EvmRpcException || it is DryRunRpcException
    }

    if (chainError != null) {
        val reason = chainError.message?.takeIf { it.isNotBlank() }
        return resourceManager.getString(R.string.common_error_general_title) to
            (reason ?: resourceManager.getString(R.string.network_fee_error_message))
    }

    val connectionFailed = causes.any {
        it is IOException || it is ConnectionClosedException || it is NovaException && it.kind == NovaException.Kind.NETWORK
    }
    return if (connectionFailed) {
        resourceManager.getString(R.string.choose_amount_network_error) to resourceManager.getString(R.string.choose_amount_error_fee)
    } else {
        resourceManager.getString(R.string.common_error_general_title) to resourceManager.getString(R.string.network_fee_error_message)
    }
}
