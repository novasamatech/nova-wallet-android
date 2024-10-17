package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
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
    }

    fun switchTextTo(model: AssetViewModeModel) {
        switchTextTo(model.iconRes, model.textRes)
    }

    fun switchTextTo(@DrawableRes iconRes: Int, @StringRes textRes: Int) {
        assetViewModeIcon.setImageResource(iconRes)
        assetViewModeText.setText(context.getText(textRes))
    }
}

class AssetViewModeModel(@DrawableRes val iconRes: Int, @StringRes val textRes: Int)
