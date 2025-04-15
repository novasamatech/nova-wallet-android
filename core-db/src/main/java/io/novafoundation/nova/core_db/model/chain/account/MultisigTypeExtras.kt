package io.novafoundation.nova.core_db.model.chain.account

import com.google.gson.JsonArray
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
    @JsonAdapter(AccountIdKeyListAdapter::class)
    val otherSignatories: List<AccountIdKey>,
    val threshold: Int
)

private class AccountIdSerializer : JsonSerializer<AccountIdKey>, JsonDeserializer<AccountIdKey> {
    override fun serialize(src: AccountIdKey, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toHex())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): AccountIdKey {
        return AccountIdKey.fromHex(json.asString).getOrThrow()
    }
}

private class AccountIdKeyListAdapter : JsonSerializer<List<AccountIdKey>>, JsonDeserializer<List<AccountIdKey>> {
    private val delegate = AccountIdSerializer()

    override fun serialize(
        src: List<AccountIdKey>,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonArray = JsonArray()
        src.forEach { jsonArray.add(delegate.serialize(it, AccountIdKey::class.java, context)) }
        return jsonArray
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<AccountIdKey> {
        return json.asJsonArray.map {
            delegate.deserialize(it, AccountIdKey::class.java, context)
        }
    }
}
