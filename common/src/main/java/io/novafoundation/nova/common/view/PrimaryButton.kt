package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.isProgressActive
import com.github.razir.progressbutton.showProgress
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.getColorFromAttr
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableFromColors

enum class ButtonState(val viewEnabled: Boolean) {
    NORMAL(true),
    DISABLED(false),
    PROGRESS(false)
}

class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatTextView(context, attrs, defStyle) {

    enum class Appearance {
        FILL, OUTLINE
    }

    private var cachedText: String? = null

    private var preparedForProgress = false

    init {
        attrs?.let(this::applyAttrs)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PrimaryButton) { typedArray ->
        val appearance = typedArray.getEnum(R.styleable.PrimaryButton_appearance, Appearance.FILL)
        setAppearance(appearance)
    }

    fun setAppearance(appearance: Appearance) = with(context) {
        val baseBackground = when (appearance) {
            Appearance.FILL -> {
                val fillColor = getColorFromAttr(R.attr.colorAccent)
                val activeState = getRoundedCornerDrawableFromColors(fillColor = fillColor)

                val disabledState = getRoundedCornerDrawable(fillColorRes = R.color.gray3)

                getCornersStateDrawable(
                    disabledDrawable = disabledState,
                    focusedDrawable = activeState,
                    idleDrawable = activeState
                )
            }
            Appearance.OUTLINE -> getIdleDrawable()
        }

        val rippleColor = getColorFromAttr(R.attr.colorControlHighlight)
        val background = addRipple(baseBackground, rippleColor = rippleColor)

        setBackground(background)
    }

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.bindProgressButton(this)

        preparedForProgress = true
    }

    fun setState(state: ButtonState) {
        isEnabled = state.viewEnabled

        if (state == ButtonState.PROGRESS) {
            checkPreparedForProgress()

            showProgress()
        } else {
            hideProgress()
        }
    }

    private fun checkPreparedForProgress() {
        if (!preparedForProgress) {
            throw IllegalArgumentException("You must call prepareForProgress() first!")
        }
    }

    private fun hideProgress() {
        if (isProgressActive()) {
            hideProgress(cachedText)
        }
    }

    private fun showProgress() {
        if (isProgressActive()) return

        cachedText = text.toString()

        showProgress {
            progressColorRes = R.color.gray2
        }
    }
}

fun PrimaryButton.setProgress(show: Boolean) {
    setState(if (show) ButtonState.PROGRESS else ButtonState.NORMAL)
}
