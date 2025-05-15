package io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.model

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.CardCredentialsResponse
import io.novafoundation.nova.feature_pay_impl.data.raise.common.network.CardValue

class RaiseCardRemote(
    @SerializedName("brand_id")
    val brandId: String,
    override val number: CardValue?,
    @SerializedName("csc")
    override val pin: CardValue?,
    override val url: CardValue?,
    val balance: Long,
    val currency: String,
    @SerializedName("expires_at")
    val expiresAt: String
) : CardCredentialsResponse
