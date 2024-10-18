package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_assets.R
import kotlinx.android.synthetic.main.view_asset_view_mode.view.assetViewModeIcon
import kotlinx.android.synthetic.main.view_asset_view_mode.view.assetViewModeText

class AssetViewModeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    private var slideAnimationBottom = true

    private val animationListener = object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {
            isClickable = false
        }

        override fun onAnimationEnd(animation: Animation?) {
            isClickable = true
        }

        override fun onAnimationRepeat(animation: Animation?) {

        }
    }

    private val slideTopInAnimation = AnimationUtils.loadAnimation(context, R.anim.asset_mode_slide_top_in).applyInterpolator()
    private val slideTopOutAnimation = AnimationUtils.loadAnimation(context, R.anim.asset_mode_slide_top_out).applyInterpolator()
    private val slideBottomInAnimation = AnimationUtils.loadAnimation(context, R.anim.asset_mode_slide_bottom_in).applyInterpolator()
    private val slideBottomOutAnimation = AnimationUtils.loadAnimation(context, R.anim.asset_mode_slide_bottom_out).applyInterpolator()

    init {
        View.inflate(context, R.layout.view_asset_view_mode, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        assetViewModeIcon.setFactory {
            val imageView = ImageView(context, null, 0)
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView
        }

        assetViewModeText.setFactory {
            val textView = TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_SemiBold_Title3)
            textView.setGravity(Gravity.CLIP_VERTICAL)
            textView.setTextColorRes(R.color.text_primary)
            textView
        }

        slideTopInAnimation.setAnimationListener(animationListener)
        slideBottomInAnimation.setAnimationListener(animationListener)
    }

    fun switchTextTo(model: AssetViewModeModel) {
        switchTextTo(model.iconRes, model.textRes)
    }

    fun switchTextTo(@DrawableRes iconRes: Int, @StringRes textRes: Int) {
        if (!shouldPlayAnimation(textRes)) return

        assetViewModeIcon.setImageResource(iconRes)

        assetViewModeText.currentView

        if (slideAnimationBottom) {
            assetViewModeText.inAnimation = slideBottomInAnimation
            assetViewModeText.outAnimation = slideBottomOutAnimation
        } else {
            assetViewModeText.inAnimation = slideTopInAnimation
            assetViewModeText.outAnimation = slideTopOutAnimation
        }

        slideAnimationBottom = !slideAnimationBottom

        assetViewModeText.setText(context.getText(textRes))
    }

    private fun Animation.applyInterpolator(): Animation {
        interpolator = AnticipateOvershootInterpolator(2.0f)
        return this
    }

    private fun shouldPlayAnimation(@StringRes textRes: Int): Boolean {
        val currentTextView = assetViewModeText.currentView as? TextView ?: return true

        return currentTextView.text != context.getString(textRes)
    }
}

data class AssetViewModeModel(@DrawableRes val iconRes: Int, @StringRes val textRes: Int)
