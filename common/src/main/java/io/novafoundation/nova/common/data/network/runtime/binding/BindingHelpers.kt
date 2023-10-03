package io.novafoundation.nova.common.data.network.runtime.binding

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.RuntimeType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.Type
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromByteArray
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.RuntimeMetadata
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

annotation class UseCaseBinding

annotation class HelperBinding

fun incompatible(): Nothing = throw IllegalStateException("Binding is incompatible")

typealias Binder<T> = (scale: String?, RuntimeSnapshot) -> T
typealias NonNullBinder<T> = (scale: String, RuntimeSnapshot) -> T
typealias NonNullBinderWithType<T> = (scale: String, RuntimeSnapshot, Type<*>) -> T
typealias BinderWithType<T> = (scale: String?, RuntimeSnapshot, Type<*>) -> T

@OptIn(ExperimentalContracts::class)
inline fun <reified T> requireType(dynamicInstance: Any?): T {
    contract {
        returns() implies (dynamicInstance is T)
    }

    return dynamicInstance as? T ?: incompatible()
}

inline fun <reified T> Any?.cast(): T {
    return this as? T ?: incompatible()
}

inline fun <reified T> Any?.nullableCast(): T? {
    if (this == null) return null

    return this as? T ?: incompatible()
}

inline fun <reified T> Any?.castOrNull(): T? {
    return this as? T
}

@OptIn(ExperimentalContracts::class)
fun Any?.castToStruct(): Struct.Instance {
    contract {
        returns() implies (this@castToStruct is Struct.Instance)
    }

    return cast()
}

@OptIn(ExperimentalContracts::class)
fun Any?.castToDictEnum(): DictEnum.Entry<*> {
    contract {
        returns() implies (this@castToDictEnum is DictEnum.Entry<*>)
    }

    return cast()
}

fun Any?.castToStructOrNull(): Struct.Instance? {
    return castOrNull()
}

fun Any?.castToList(): List<*> {
    return cast()
}

inline fun <reified R> Struct.Instance.getTyped(key: String) = get<R>(key) ?: incompatible()

fun Struct.Instance.getList(key: String) = get<List<*>>(key) ?: incompatible()
fun Struct.Instance.getStruct(key: String) = get<Struct.Instance>(key) ?: incompatible()

inline fun <T> bindOrNull(binder: () -> T): T? = runCatching(binder).getOrNull()

fun StorageEntry.returnType() = type.value ?: incompatible()

fun RuntimeMetadata.storageReturnType(moduleName: String, storageName: String): Type<*> {
    return module(moduleName).storage(storageName).returnType()
}

fun <D> RuntimeType<*, D>.fromHexOrIncompatible(scale: String, runtime: RuntimeSnapshot): D = successOrIncompatible {
    fromHex(runtime, scale)
}

fun <D> RuntimeType<*, D>.fromByteArrayOrIncompatible(scale: ByteArray, runtime: RuntimeSnapshot): D = successOrIncompatible {
    fromByteArray(runtime, scale)
}

private fun <T> successOrIncompatible(block: () -> T) : T = runCatching {
    block()
}.getOrElse { incompatible() }
