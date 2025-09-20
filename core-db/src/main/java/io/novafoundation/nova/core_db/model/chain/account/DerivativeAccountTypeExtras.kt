package io.novafoundation.nova.core_db.model.chain.account

import com.google.gson.annotations.JsonAdapter
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AccountIdSerializer

class DerivativeAccountTypeExtras(
    val index: Int,
    @JsonAdapter(AccountIdSerializer::class)
    val parentAccountId: AccountIdKey
)
