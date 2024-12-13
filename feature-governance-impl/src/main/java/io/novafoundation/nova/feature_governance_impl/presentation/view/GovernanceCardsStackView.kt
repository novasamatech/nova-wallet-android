package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.yuyakaido.android.cardstackview.CardStackView

class GovernanceCardsStackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : CardStackView(context, attrs, defStyle) {

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if (!isEnabled) return false

        return super.onTouchEvent(e)
    }
}
