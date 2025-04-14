package io.novafoundation.nova.core_db.model.chain.account

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHex
import io.novafoundation.nova.common.address.toHex
import java.lang.reflect.Type

class MultisigTypeExtras(
    val signatoryMetaId: Long,
    val otherSignatories: List<AccountIdKey>,
    val threshold: Int
) {

    class Signatory(@JsonAdapter(AccountIdSerializer::class) val accountIdKey: AccountIdKey)
}

class AccountIdSerializer : JsonSerializer<AccountIdKey>, JsonDeserializer<AccountIdKey> {
    override fun serialize(src: AccountIdKey, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toHex())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): AccountIdKey {
        return AccountIdKey.fromHex(json.asString).getOrThrow()
    }
}
