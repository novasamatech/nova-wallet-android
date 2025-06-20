package io.novafoundation.nova.runtime.ethereum.gas.etherscan

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.utils.Gwei

class EtherscanGasResponse(
    @SerializedName("ProposeGasPrice")
    val proposeGasPrice: Gwei,
    val suggestBaseFee: Gwei
)
