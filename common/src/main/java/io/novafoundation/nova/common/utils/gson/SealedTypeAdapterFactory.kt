package io.novafoundation.nova.common.utils.gson

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.reflect.KClass

class SealedTypeAdapterFactory<T : Any> private constructor(
    private val baseType: KClass<T>,
    private val typeFieldName: String
) : TypeAdapterFactory {

    private val subclasses = baseType.sealedSubclasses
    private val nameToSubclass = subclasses.associateBy { it.simpleName!! }

    init {
        if (!baseType.isSealed) throw IllegalArgumentException("$baseType is not a sealed class")
    }

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>?): TypeAdapter<R>? {
        if (type == null || subclasses.isEmpty() || subclasses.none { type.rawType.isAssignableFrom(it.java) }) return null

        val elementTypeAdapter = gson.getAdapter(JsonElement::class.java)
        val subclassToDelegate: Map<KClass<*>, TypeAdapter<*>> = subclasses.associateWith {
            gson.getDelegateAdapter(this, TypeToken.get(it.java))
        }

        return object : TypeAdapter<R>() {
            override fun write(writer: JsonWriter, value: R) {
                val srcType = value::class
                val label = srcType.simpleName!!
                @Suppress("UNCHECKED_CAST") val delegate = subclassToDelegate[srcType] as TypeAdapter<R>
                val jsonObject = delegate.toJsonTree(value).asJsonObject

                if (jsonObject.has(typeFieldName)) {
                    throw JsonParseException("cannot serialize $label because it already defines a field named $typeFieldName")
                }
                val clone = JsonObject()
                clone.add(typeFieldName, JsonPrimitive(label))
                jsonObject.entrySet().forEach {
                    clone.add(it.key, it.value)
                }
                elementTypeAdapter.write(writer, clone)
            }

            override fun read(reader: JsonReader): R {
                val element = elementTypeAdapter.read(reader)
                val labelElement = element.asJsonObject.remove(typeFieldName) ?: throw JsonParseException(
                    "cannot deserialize $baseType because it does not define a field named $typeFieldName"
                )
                val name = labelElement.asString
                val subclass = nameToSubclass[name] ?: throw JsonParseException("cannot find $name subclass of $baseType")
                @Suppress("UNCHECKED_CAST")
                return (subclass.objectInstance as? R) ?: (subclassToDelegate[subclass]!!.fromJsonTree(element) as R)
            }
        }
    }

    companion object {
        fun <T : Any> of(clz: KClass<T>) = SealedTypeAdapterFactory(clz, "type")
    }
}
