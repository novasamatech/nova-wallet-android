package io.novafoundation.nova.common.data.network.rpc

import io.novasama.substrate_sdk_android.extensions.toHexString
import java.io.ByteArrayOutputStream

private const val CHILD_KEY_DEFAULT = ":child_storage:default:"

suspend fun childStateKey(
    builder: suspend ByteArrayOutputStream.() -> Unit
): String {
    val buffer = ByteArrayOutputStream().apply {
        write(CHILD_KEY_DEFAULT.encodeToByteArray())

        builder()
    }

    return buffer.toByteArray().toHexString(withPrefix = true)
}
