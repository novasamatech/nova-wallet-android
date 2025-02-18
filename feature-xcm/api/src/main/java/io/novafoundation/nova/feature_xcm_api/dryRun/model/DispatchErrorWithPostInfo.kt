package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.RuntimeContext

class DispatchErrorWithPostInfo(
    val postInfo: DispatchPostInfo,
    val error: DispatchError
) {

    companion object {

        context(RuntimeContext)
        fun bind(decodedInstance: Any?): DispatchErrorWithPostInfo {
            val asStruct = decodedInstance.castToStruct()

            return DispatchErrorWithPostInfo(
                postInfo = DispatchPostInfo.bind(asStruct["post_info"]),
                error = bindDispatchError(asStruct["error"])
            )
        }
    }
}
