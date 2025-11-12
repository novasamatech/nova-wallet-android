package io.novafoundation.nova.feature_gift_impl.presentation.share

import androidx.annotation.RawRes

class ShareGiftAnimationState(
    @RawRes val res: Int,
    val state: State
) {
    enum class State {
        START,
        IDLE_END
    }
}
