package io.novafoundation.nova.feature_wallet_impl.data.network.etherscan.model

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.utils.removeHexPrefix
import java.math.BigInteger

class EtherscanNormalTxResponse(
    val timeStamp: Long,
    val hash: String,
    val from: String,
    val to: String,
    val value: BigInteger,
    val input: String,
    val functionName: String,
    @SerializedName("txreceipt_status")val txReceiptStatus: BigInteger,
    override val gasPrice: BigInteger,
    override val gasUsed: BigInteger,
) : WithEvmFee

val EtherscanNormalTxResponse.isTransfer
    get() = input.removeHexPrefix().isEmpty()
