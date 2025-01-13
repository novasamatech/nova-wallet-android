package io.novafoundation.nova.common.view.recyclerview.item

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ItemOperationListItemBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setImageTintRes

class OperationListItem @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    enum class IconStyle {
        BORDERED_CIRCLE, DEFAULT
    }

    private val binder = ItemOperationListItemBinding.inflate(inflater(), this)

    val icon: ImageView
        get() = binder.itemOperationIcon

    val header: TextView
        get() = binder.itemOperationHeader

    val subHeader: TextView
        get() = binder.itemOperationSubHeader

    val valuePrimary: TextView
        get() = binder.itemOperationValuePrimary

    val valueSecondary: TextView
        get() = binder.itemOperationValueSecondary

    val status: ImageView
        get() = binder.itemOperationValueStatus

    init {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)

        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setIconStyle(iconStyle: IconStyle) {
        when (iconStyle) {
            IconStyle.BORDERED_CIRCLE -> {
                icon.setBackgroundResource(R.drawable.bg_icon_container_on_color)
                icon.setImageTintRes(R.color.icon_secondary)
            }
            IconStyle.DEFAULT -> {
                icon.setPadding(0)
                icon.background = null
                icon.setImageTint(null)
            }
        }
    }
}
