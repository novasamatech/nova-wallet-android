package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toRect
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.zxing.qrcode.encoder.QRCode
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.formatIcon

class QrCodeModel(
    val qrCode: QRCode,
    val overlayBackground: Drawable?,
    val overlayPaddingInDp: Int,
    val centerOverlay: Icon,
)

class QrCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val qrColor = context.getColor(R.color.qr_code_content)
    private val backgroundColor = context.getColor(R.color.qr_code_background)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var data: QRCode? = null

    private var overlayPadding: Int = 0
    private var centerOverlay: Drawable? = null
    private var overlayBackground: Drawable? = null

    private val overlaySize = 64.dp
    private val overlayQuiteZone = 4.dp
    private val qrPadding = 16.dpF

    private val centerRect: RectF = RectF()

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        if (isInEditMode) {
            ImageLoader.invoke(context)
        } else {
            FeatureUtils.getCommonApi(context).imageLoader()
        }
    }

    fun setQrModel(model: QrCodeModel) {
        this.data = model.qrCode
        this.overlayBackground = model.overlayBackground
        this.overlayPadding = model.overlayPaddingInDp.dp(context)

        if (model.centerOverlay != null) {
            val centerOverlayRequest = getCenterOverlayImageRequest(model.centerOverlay) {
                this.centerOverlay = it
                invalidate()
            }

            imageLoader.enqueue(centerOverlayRequest)
        }
        invalidate()
    }

    private fun getCenterOverlayImageRequest(icon: Icon, target: (Drawable?) -> Unit): ImageRequest {
        return ImageRequest.Builder(context)
            .data(ImageLoader.formatIcon(icon))
            .target(onSuccess = target)
            .size(overlaySize, overlaySize)
            .build()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(widthSize, heightSize)

        centerRect.left = (measuredWidth / 2 - overlaySize / 2).toFloat()
        centerRect.top = (measuredHeight / 2 - overlaySize / 2).toFloat()
        centerRect.right = (measuredWidth / 2 + overlaySize / 2).toFloat()
        centerRect.bottom = (measuredHeight / 2 + overlaySize / 2).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()

        paint.color = qrColor
        canvas.drawColor(backgroundColor)
        data?.let { renderQRCodeImage(canvas, it) }

        canvas.restore()
    }

    private fun renderQRCodeImage(canvas: Canvas, data: QRCode) {
        renderQRImage(canvas, data, width, height)
    }

    private fun renderQRImage(canvas: Canvas, code: QRCode, width: Int, height: Int) {
        paint.color = qrColor
        val input = code.matrix ?: throw IllegalStateException()
        val inputWidth = input.width
        val inputHeight = input.height
        val outputWidth = Math.max(width, inputWidth) - qrPadding * 2
        val outputHeight = Math.max(height, inputHeight) - qrPadding * 2
        val multiple = Math.min(outputWidth / inputWidth, outputHeight / inputHeight)
        val leftPadding = qrPadding
        val topPadding = qrPadding
        val FINDER_PATTERN_SIZE = 7f
        val CIRCLE_SCALE_DOWN_FACTOR = 0.7f
        val circleSize = (multiple * CIRCLE_SCALE_DOWN_FACTOR)
        val circleRadius = circleSize / 2

        var inputY = 0
        var outputY = topPadding

        while (inputY < inputHeight) {
            var inputX = 0
            var outputX = leftPadding
            while (inputX < inputWidth) {
                if (input[inputX, inputY].toInt() == 1) {
                    val overlaysFinder = inputX <= FINDER_PATTERN_SIZE && inputY <= FINDER_PATTERN_SIZE ||
                        inputX >= inputWidth - FINDER_PATTERN_SIZE && inputY <= FINDER_PATTERN_SIZE ||
                        inputX <= FINDER_PATTERN_SIZE && inputY >= inputHeight - FINDER_PATTERN_SIZE

                    val overlaysCenter = (this.overlay != null) && (
                        this.centerRect.intersects(
                            outputX - overlayQuiteZone,
                            outputY - overlayQuiteZone,
                            outputX + circleSize + overlayQuiteZone,
                            outputY + circleRadius + overlayQuiteZone
                        )
                        )

                    if (!overlaysCenter && !overlaysFinder) {
                        canvas.drawCircle(paddingStart + outputX + circleRadius, paddingStart + outputY + circleRadius, circleRadius, paint)
                    }
                }
                inputX++
                outputX += multiple
            }
            inputY++
            outputY += multiple
        }
        val cornerCircleDiameter = multiple * FINDER_PATTERN_SIZE
        drawFinderPatternCircleStyle(canvas, leftPadding, topPadding, cornerCircleDiameter)
        drawFinderPatternCircleStyle(canvas, leftPadding + (inputWidth - FINDER_PATTERN_SIZE) * multiple, topPadding, cornerCircleDiameter)
        drawFinderPatternCircleStyle(canvas, leftPadding, topPadding + (inputHeight - FINDER_PATTERN_SIZE) * multiple, cornerCircleDiameter)

        drawCenterOverlay(canvas)
    }

    private fun drawFinderPatternCircleStyle(canvas: Canvas, x: Float, y: Float, circleDiameter: Float) {
        val radius = circleDiameter / 2
        val WHITE_CIRCLE_RADIUS = circleDiameter * 5 / 7 / 2
        val WHITE_CIRCLE_OFFSET = circleDiameter / 7 + WHITE_CIRCLE_RADIUS
        val MIDDLE_DOT_RADIUS = circleDiameter * 3 / 7 / 2
        val MIDDLE_DOT_OFFSET = circleDiameter * 2 / 7 + MIDDLE_DOT_RADIUS
        paint.color = qrColor
        canvas.drawCircle(x + radius, y + radius, radius, paint)
        paint.color = backgroundColor
        canvas.drawCircle(x + WHITE_CIRCLE_OFFSET, y + WHITE_CIRCLE_OFFSET, WHITE_CIRCLE_RADIUS, paint)
        paint.color = qrColor
        canvas.drawCircle(x + MIDDLE_DOT_OFFSET, y + MIDDLE_DOT_OFFSET, MIDDLE_DOT_RADIUS, paint)
    }

    private fun drawCenterOverlay(canvas: Canvas) {
        val overlayBackgroundWithInsets = centerRect.toRect().apply {
            inset(overlayPadding, overlayPadding)
        }

        overlayBackground?.bounds = overlayBackgroundWithInsets
        centerOverlay?.bounds = overlayBackgroundWithInsets

        overlayBackground?.draw(canvas)
        centerOverlay?.draw(canvas)
    }
}
