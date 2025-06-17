package io.novafoundation.nova.core_db.model.chain.account

import com.google.gson.annotations.JsonAdapter
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AccountIdKeyListAdapter
import io.novafoundation.nova.common.address.AccountIdSerializer

class MultisigTypeExtras(
    @JsonAdapter(AccountIdKeyListAdapter::class)
    val otherSignatories: List<AccountIdKey>,
    val threshold: Int,
    @JsonAdapter(AccountIdSerializer::class)
    val signatoryAccountId: AccountIdKey
)
