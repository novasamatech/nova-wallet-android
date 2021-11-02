package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.utils.system
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

enum class ExtrinsicStatusEvent {
    SUCCESS, FAILURE
}

@UseCaseBinding
fun bindEventRecords(
    scale: String,
    runtime: RuntimeSnapshot,
): List<EventRecord> {
    val returnType = runtime.metadata.system().storage("Events").type.value ?: incompatible()

    val dynamicInstance = returnType.fromHex(runtime, scale)
    requireType<List<*>>(dynamicInstance)

    return dynamicInstance.mapNotNull { dynamicEventRecord ->
        bindEventRecord(dynamicEventRecord)
    }
}
