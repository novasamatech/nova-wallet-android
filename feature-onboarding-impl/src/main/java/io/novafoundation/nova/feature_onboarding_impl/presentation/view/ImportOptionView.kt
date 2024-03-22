package io.novafoundation.nova.feature_onboarding_impl.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_onboarding_impl.R
import kotlinx.android.synthetic.main.view_import_option.view.importOptionDescription
import kotlinx.android.synthetic.main.view_import_option.view.importOptionImage
import kotlinx.android.synthetic.main.view_import_option.view.importOptionName

class ImportOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : MaterialCardView(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.dpF // It will be drawn along border with clipping and finally will be viewed as 1dp
        color = context.getColor(R.color.container_border)
    }

    init {
        setCardBackgroundColor(context.getColor(R.color.button_background_secondary))
        radius = 12.dpF
        cardElevation = 0.dpF
        elevation = 0.dpF

        View.inflate(context, R.layout.view_import_option, this)

        attrs?.let(::applyAttributes)

        setWillNotDraw(true)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        canvas.save()
        canvas.clipRect(importOptionImage.left, importOptionImage.top, importOptionImage.right, importOptionImage.bottom)
        canvas.drawRoundRect(
            importOptionImage.left.toFloat(),
            importOptionImage.top.toFloat(),
            importOptionImage.right.toFloat(),
            importOptionImage.bottom.toFloat() + radius,
            radius,
            radius,
            strokePaint
        )
        canvas.restore()
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.ImportOptionView) {
        if (it.hasValue(R.styleable.ImportOptionView_android_src)) {
            importOptionImage.setImageDrawable(it.getDrawable(R.styleable.ImportOptionView_android_src))
        }

        importOptionName.text = it.getString(R.styleable.ImportOptionView_title)
        importOptionDescription.text = it.getString(R.styleable.ImportOptionView_android_text)
    }
}
