package io.novafoundation.nova.runtime.extrinsic

import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder


fun ExtrinsicBuilder.systemRemark(remark: ByteArray): ExtrinsicBuilder {

    return call(
        moduleName = "System",
        callName = "remark",
        arguments = mapOf(
            "remark" to remark
        )
    )
}
