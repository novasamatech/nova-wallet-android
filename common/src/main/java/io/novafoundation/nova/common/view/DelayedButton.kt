package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.isProgressActive
import com.github.razir.progressbutton.showProgress
import com.google.android.material.button.MaterialButton
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.textOrNull
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withClip

class DelayedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : MaterialButton(context, attrs, defStyle) {
    private var delayDuration: Long = 5000
    private var progressWidth: Float = 0f
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var timer: CountDownTimer? = null
    private val rectF = RectF()

    private var originalText: CharSequence? = null
    private val timeFormat = "%02d:%02d"

    init {
        context.withStyledAttributes(attrs, R.styleable.DelayedMaterialButton) {
            delayDuration = getInt(R.styleable.DelayedMaterialButton_delayDuration, 5000).toLong()
            val pColor = getColor(
                R.styleable.DelayedMaterialButton_progressColor,
                ContextCompat.getColor(context, android.R.color.white)
            )
            progressPaint.color = pColor
        }

        originalText = text
        isEnabled = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (timer == null && w > 0) {
            startTimer()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(delayDuration, 16) {
            override fun onTick(millisUntilFinished: Long) {
                val progress = (delayDuration - millisUntilFinished).toFloat() / delayDuration
                progressWidth = width * progress

                val totalSeconds = (millisUntilFinished + 999) / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                text = timeFormat.format(minutes, seconds)

                invalidate()
            }

            override fun onFinish() {
                progressWidth = 0f
                isEnabled = true
                text = originalText
                invalidate()
            }
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        if (!isEnabled && progressWidth > 0) {
            val radius = cornerRadius.toFloat()
            rectF.set(0f, 0f, progressWidth, height.toFloat())
            canvas.withClip(rectF) {
                drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, progressPaint)
            }
        }
        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        timer?.cancel()
    }
}
