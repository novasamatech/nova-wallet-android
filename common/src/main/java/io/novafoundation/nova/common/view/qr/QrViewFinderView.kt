package io.novafoundation.nova.common.view.qr

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRectF
import com.journeyapps.barcodescanner.CameraPreview
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getDrawableCompat

typealias OnFramingRectChangeListener = (Rect) -> Unit

class QrViewFinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maskColor = context.getColor(R.color.dim_background)
    private val whiteColor = context.getColor(R.color.text_primary)

    // Cache the framingRect so that we can still draw it after the preview
    // stopped.
    var framingRect: Rect? = null
        private set

    private val framingPath = Path()

    private var cameraPreview: CameraPreview? = null

    private val finderDrawable: Drawable = context.getDrawableCompat(R.drawable.ic_view_finder)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornerRadius = 25.dpF(context)

    private var frameChangeListener: OnFramingRectChangeListener? = null

    fun onFinderRectChanges(listener: (Rect) -> Unit) {
        frameChangeListener = listener
    }

    fun setCameraPreview(cameraPreview: CameraPreview) {
        this.cameraPreview = cameraPreview

        cameraPreview.addStateListener(object : CameraPreview.StateListener {
            override fun previewSized() {
                refreshSizes()
                invalidate()
            }

            override fun previewStarted() {}
            override fun previewStopped() {}
            override fun cameraError(error: Exception?) {}
            override fun cameraClosed() {}
        })
    }

    override fun onDraw(canvas: Canvas) {
        if (framingRect == null) {
            return
        }

        paint.color = maskColor
        canvas.drawPath(framingPath, paint)

        paint.color = whiteColor
        finderDrawable.bounds = framingRect!!
        finderDrawable.draw(canvas)
    }

    private fun refreshSizes() {
        if (cameraPreview == null) {
            return
        }

        val framingRect = cameraPreview!!.framingRect

        if (framingRect != null) {
            this.framingRect = framingRect

            frameChangeListener?.invoke(framingRect)

            framingPath.reset()
            framingPath.addRoundRect(framingRect.toRectF(), cornerRadius, cornerRadius, Path.Direction.CW)
            framingPath.fillType = Path.FillType.INVERSE_EVEN_ODD
            framingPath.close()
        }
    }
}
