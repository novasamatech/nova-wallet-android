package io.novafoundation.nova.runtime.extrinsic

import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder



fun ExtrinsicBuilder.systemRemark(remark: ByteArray): ExtrinsicBuilder {
    return call(
        moduleName = "System",
        callName = "remark",
        arguments = mapOf(
            "remark" to remark
        )
    )
}

fun ExtrinsicBuilder.systemRemarkWithEvent(remark: ByteArray): ExtrinsicBuilder {
    return call(
        moduleName = "System",
        callName = "remark_with_event",
        arguments = mapOf(
            "remark" to remark
        )
    )
}

fun ExtrinsicBuilder.systemRemarkWithEvent(remark: String): ExtrinsicBuilder {
    return systemRemarkWithEvent(remark.encodeToByteArray())
}
