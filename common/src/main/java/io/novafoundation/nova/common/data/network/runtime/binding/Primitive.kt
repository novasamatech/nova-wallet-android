package io.novafoundation.nova.common.data.network.runtime.binding

import java.math.BigInteger

@HelperBinding
fun bindNumber(dynamicInstance: Any?): BigInteger = dynamicInstance.cast()

@HelperBinding
fun bindDoubleOrNull(dynamicInstance: Any?): Double? = dynamicInstance.castOrNull()

@HelperBinding
fun bindString(dynamicInstance: Any?): String = dynamicInstance.cast<ByteArray>().decodeToString()

@HelperBinding
fun bindBoolean(dynamicInstance: Any?): Boolean = dynamicInstance.cast()
