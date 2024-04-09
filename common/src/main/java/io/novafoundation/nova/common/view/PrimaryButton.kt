package io.novafoundation.nova.common.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.LifecycleOwner
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.isProgressActive
import com.github.razir.progressbutton.showProgress
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
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

private const val ICON_SIZE_DP_DEFAULT = 24
private const val ICON_PADDING_DP_DEFAULT = 8

class PrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Nova_Button), attrs, defStyle) {

    enum class Appearance {

        PRIMARY {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_primary)
        },
        PRIMARY_TRANSPARENT {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive_on_gradient)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_primary)
        },
        SECONDARY {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_secondary)
        },
        SECONDARY_TRANSPARENT {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive_on_gradient)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_secondary)
        },

        PRIMARY_POSITIVE {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_approve)
        },

        PRIMARY_NEGATIVE {
            override fun disabledColor(context: Context) = context.getColor(R.color.button_background_inactive)

            override fun enabledColor(context: Context) = context.getColor(R.color.button_background_reject)
        },

        ACCENT_SECONDARY_TRANSPARENT {
            override fun enabledColor(context: Context): Int = context.getColor(R.color.button_background_secondary)

            override fun disabledColor(context: Context): Int = context.getColor(R.color.button_background_inactive_on_gradient)

            override fun textColor(context: Context): ColorStateList = context.getColorStateList(R.color.button_accent_text_colors)
        };

        @ColorInt
        abstract fun disabledColor(context: Context): Int

        @ColorInt
        abstract fun enabledColor(context: Context): Int

        open fun textColor(context: Context): ColorStateList = context.getColorStateList(R.color.button_text_colors)
    }

    enum class Size(val heightDp: Int, val cornerSizeDp: Int, @StyleRes val textAppearance: Int) {
        LARGE(52, 12, R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline),
        SMALL(44, 10, R.style.TextAppearance_NovaFoundation_SemiBold_SubHeadline),
        EXTRA_SMALL(32, 10, R.style.TextAppearance_NovaFoundation_SemiBold_Footnote);
    }

    private var cachedText: String? = null

    private lateinit var size: Size

    private var preparedForProgress = false

    private var icon: Bitmap? = null
    private var iconPaint: Paint? = null
    private var iconSrcRect: Rect? = null
    private var iconDestRect: Rect? = null
    private var iconPadding = 0
    private var iconSize = 0

    init {
        attrs?.let(this::applyAttrs)
    }

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.bindProgressButton(this)

        preparedForProgress = true
    }

    override fun onDraw(canvas: Canvas) {
        val icon = icon

        if (icon != null && !isProgressActive()) {
            val shift: Int = (iconSize + iconPadding) / 2

            canvas.save()
            canvas.translate(shift.toFloat(), 0f)

            super.onDraw(canvas)

            val textWidth = paint.measureText(text.toString())
            val left = (width / 2f - textWidth / 2f - iconSize - iconPadding).toInt()
            val top: Int = height / 2 - iconSize / 2

            iconDestRect!!.set(left, top, left + iconSize, top + iconSize)
            canvas.drawBitmap(icon, iconSrcRect, iconDestRect!!, iconPaint)

            canvas.restore()
        } else {
            super.onDraw(canvas)
        }
    }

    fun showProgress(show: Boolean) {
        isEnabled = !show

        if (show) {
            checkPreparedForProgress()

            showProgress()
        } else {
            hideProgress()
        }
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

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.PrimaryButton) { typedArray ->
        size = typedArray.getEnum(R.styleable.PrimaryButton_size, Size.LARGE)
        setTextAppearance(size.textAppearance)

        minimumHeight = size.heightDp.dp(context)

        typedArray.getDrawable(R.styleable.PrimaryButton_iconSrc)?.let { icon = drawableToBitmap(it) }
        icon?.let { icon ->
            iconPadding = typedArray.getDimensionPixelSize(R.styleable.PrimaryButton_iconPadding, ICON_PADDING_DP_DEFAULT.dp(context))
            iconSize = typedArray.getDimensionPixelSize(R.styleable.PrimaryButton_iconSize, ICON_SIZE_DP_DEFAULT.dp(context))
            iconPaint = Paint()
            iconSrcRect = Rect(0, 0, icon.width, icon.height)
            iconDestRect = Rect()
        }

        val appearance = typedArray.getEnum(R.styleable.PrimaryButton_appearance, Appearance.PRIMARY)
        setAppearance(appearance, cornerSizeDp = size.cornerSizeDp)
    }

    private fun setAppearance(appearance: Appearance, cornerSizeDp: Int) = with(context) {
        val activeState = getRoundedCornerDrawableFromColors(appearance.enabledColor(this), cornerSizeInDp = cornerSizeDp)
        val baseBackground = getCornersStateDrawable(
            disabledDrawable = getRoundedCornerDrawableFromColors(appearance.disabledColor(this), cornerSizeInDp = cornerSizeDp),
            focusedDrawable = activeState,
            idleDrawable = activeState
        )

        val rippleColor = getColorFromAttr(R.attr.colorControlHighlight)
        val background = addRipple(baseBackground, mask = null, rippleColor = rippleColor)

        setBackground(background)
        setTextColor(appearance.textColor(this))
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
            progressColor = currentTextColor
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun setButtonColor(color: Int) {
        background.setTint(color)
    }
}

fun PrimaryButton.setProgressState(show: Boolean) {
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
