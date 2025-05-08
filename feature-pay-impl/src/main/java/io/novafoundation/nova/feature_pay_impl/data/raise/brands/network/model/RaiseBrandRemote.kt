package io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model

import com.google.gson.annotations.SerializedName

class RaiseBrandRemote(
    val name: String,
    val description: String,
    val terms: String,
    @SerializedName("commission_rate")
    val commissionRate: Long,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("transaction_config")
    val transactionConfig: TransactionConfig
) {

    class TransactionConfig(
        @SerializedName("variable_load")
        val variableLoad: VariableLoad?
    )

    class VariableLoad(
        @SerializedName("minimum_amount")
        val minAmount: Long,
        @SerializedName("maximum_amount")
        val maxAmount: Long
    )
}
