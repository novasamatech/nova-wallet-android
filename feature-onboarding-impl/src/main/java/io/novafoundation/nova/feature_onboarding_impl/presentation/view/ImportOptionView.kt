package io.novafoundation.nova.feature_onboarding_impl.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_onboarding_impl.R
import io.novafoundation.nova.feature_onboarding_impl.databinding.ViewImportOptionBinding

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

    private val binder = ViewImportOptionBinding.inflate(inflater(), this)

    init {
        setCardBackgroundColor(context.getColor(R.color.button_background_secondary))
        radius = 12.dpF
        cardElevation = 0.dpF
        elevation = 0.dpF

        attrs?.let(::applyAttributes)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        canvas.save()
        canvas.clipRect(binder.importOptionImage.left, binder.importOptionImage.top, binder.importOptionImage.right, binder.importOptionImage.bottom)
        canvas.drawRoundRect(
            binder.importOptionImage.left.toFloat(),
            binder.importOptionImage.top.toFloat(),
            binder.importOptionImage.right.toFloat(),
            binder.importOptionImage.bottom.toFloat() + radius,
            radius,
            radius,
            strokePaint
        )
        canvas.restore()
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.ImportOptionView) {
        if (it.hasValue(R.styleable.ImportOptionView_android_src)) {
            binder.importOptionImage.setImageDrawable(it.getDrawable(R.styleable.ImportOptionView_android_src))
        }

        binder.importOptionName.text = it.getString(R.styleable.ImportOptionView_title)
        binder.importOptionDescription.text = it.getString(R.styleable.ImportOptionView_android_text)
    }
}
