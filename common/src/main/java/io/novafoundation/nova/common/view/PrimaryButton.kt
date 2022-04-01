package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.isProgressActive
import com.github.razir.progressbutton.showProgress
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.doOnGlobalLayout
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.getColorFromAttr
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableFromColors

enum class ButtonState {
    NORMAL,
    DISABLED,
    PROGRESS,
    GONE
}

class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Nova_Button), attrs, defStyle) {

    enum class Appearance {

        PRIMARY {
            override fun disabledColor(context: Context) = context.getColor(R.color.disabledColor)

            override fun enabledColor(context: Context) = context.getColorFromAttr(R.attr.colorAccent)
        },
        PRIMARY_TRANSPARENT {
            override fun disabledColor(context: Context) = context.getColor(R.color.disabledTransparent)

            override fun enabledColor(context: Context) = context.getColorFromAttr(R.attr.colorAccent)
        },
        SECONDARY {
            override fun disabledColor(context: Context) = context.getColor(R.color.disabledColor)

            override fun enabledColor(context: Context) = context.getColor(R.color.accentSecondary)
        },
        SECONDARY_TRANSPARENT {
            override fun disabledColor(context: Context) = context.getColor(R.color.disabledTransparent)

            override fun enabledColor(context: Context) = context.getColor(R.color.white_16)
        };

        @ColorInt
        abstract fun disabledColor(context: Context): Int

        @ColorInt
        abstract fun enabledColor(context: Context): Int
    }

    enum class Size(val heightDp: Int, val cornerSizeDp: Int) {
        LARGE(52, 12), SMALL(44, 10);
    }

    private var cachedText: String? = null

    private var preparedForProgress = false

    init {
        attrs?.let(this::applyAttrs)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PrimaryButton) { typedArray ->
        val appearance = typedArray.getEnum(R.styleable.PrimaryButton_appearance, Appearance.PRIMARY)
        val size = typedArray.getEnum(R.styleable.PrimaryButton_size, Size.LARGE)

        setConfiguration(size, appearance)
    }

    fun setConfiguration(size: Size, appearance: Appearance) {
        setSize(size)

        setAppearance(appearance, cornerSizeDp = size.cornerSizeDp)
    }

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.bindProgressButton(this)

        preparedForProgress = true
    }

    fun setState(state: ButtonState) {
        isEnabled = state == ButtonState.NORMAL
        setVisible(state != ButtonState.GONE)

        if (state == ButtonState.PROGRESS) {
            checkPreparedForProgress()

            showProgress()
        } else {
            hideProgress()
        }
    }

    private fun setSize(size: Size) {
        doOnGlobalLayout {
            layoutParams.apply {
                height = size.heightDp.dp(context)
            }
        }
    }

    private fun setAppearance(appearance: Appearance, cornerSizeDp: Int) = with(context) {
        val activeState = getRoundedCornerDrawableFromColors(appearance.enabledColor(this), cornerSizeInDp = cornerSizeDp)
        val baseBackground = getCornersStateDrawable(
            disabledDrawable = getRoundedCornerDrawableFromColors(appearance.disabledColor(this), cornerSizeInDp = cornerSizeDp),
            focusedDrawable = activeState,
            idleDrawable = activeState
        )

        val rippleColor = getColorFromAttr(R.attr.colorControlHighlight)
        val background = addRipple(baseBackground, rippleColor = rippleColor)

        setBackground(background)
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

fun PrimaryButton.setState(descriptiveButtonState: DescriptiveButtonState) {
    when (descriptiveButtonState) {
        is DescriptiveButtonState.Disabled -> {
            setState(ButtonState.DISABLED)
            text = descriptiveButtonState.reason
        }
        is DescriptiveButtonState.Enabled -> {
            setState(ButtonState.NORMAL)
            text = descriptiveButtonState.action
        }
        DescriptiveButtonState.Loading -> {
            setState(ButtonState.PROGRESS)
        }
        DescriptiveButtonState.Gone -> {
            setState(ButtonState.GONE)
        }
    }
}
