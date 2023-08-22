package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.orZero
import java.math.BigInteger

@HelperBinding
fun bindNumber(dynamicInstance: Any?): BigInteger = dynamicInstance.cast()

fun bindInt(dynamicInstance: Any?): Int = bindNumber(dynamicInstance).toInt()

@HelperBinding
fun bindNumberOrZero(dynamicInstance: Any?): BigInteger = dynamicInstance?.let(::bindNumber).orZero()

@HelperBinding
fun bindString(dynamicInstance: Any?): String = dynamicInstance.cast<ByteArray>().decodeToString()

@HelperBinding
fun bindBoolean(dynamicInstance: Any?): Boolean = dynamicInstance.cast()

@HelperBinding
fun bindByteArray(dynamicInstance: Any?): ByteArray = dynamicInstance.cast()
