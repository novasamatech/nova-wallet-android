package io.novafoundation.nova.common.view.paralaxCard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import io.novafoundation.nova.common.R

private const val CARD_MAX_ROTATION = 2f
private const val DEVICE_ROTATION_ANGLE = 12f

class ParalaxCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val gyroscopeListener = CardGyroscopeListener(context, DEVICE_ROTATION_ANGLE, ::onGyroscopeRotation)

    // Card background bitmap
    private val cardBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_paralax_card_background)
        .downscale(0.2f)

    // Highlights bitmaps
    private val cardHighlightBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_card_background_highlight)
    private val cardBorderHighlightBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_card_border_highlight)
    private val nestedViewBorderHighlightBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_frosted_glass_highlight)
    private val paralaxHighlightBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_pattern_highlight)

    // Shaders
    private val cardHighlightShader = cardHighlightBitmap.toBitmapShaderWithPaint()
    private val cardBorderHighlightShader = cardBorderHighlightBitmap.toBitmapShaderWithPaint()
        .apply {
            paint.strokeWidth = 10f
            paint.style = Paint.Style.STROKE
        }
    private val paralaxHighlightShader = paralaxHighlightBitmap.toBitmapShaderWithPaint()
    private val nestedViewBorderHighlightShader = nestedViewBorderHighlightBitmap.toBitmapShaderWithPaint()
        .apply {
            paint.style = Paint.Style.STROKE
        }

    // Paralax bitmaps
    private val paralaxFirstBitmap = BitmapFactory.decodeResource(resources, R.drawable.big_star)
        .convertToAlphaMask()
        .toBitmapWithRect()
    private val paralaxSecondBitmap = BitmapFactory.decodeResource(resources, R.drawable.middle_star)
        .downscale(0.2f)
        .blurBitmap(6)
        .convertToAlphaMask()
        .toBitmapWithRect()
    private val paralaxThirdBitmap = BitmapFactory.decodeResource(resources, R.drawable.smallest_star)
        .downscale(0.1f)
        .blurBitmap(8)
        .convertToAlphaMask()
        .toBitmapWithRect()

    // Paralax bitmaps blurred
    private val paralaxFirstBlurredBitmap = paralaxFirstBitmap.bitmap
        .downscale(0.2f)
        .blurBitmap(12)
        .convertToAlphaMask()
        .toBitmapWithRect()
    private val paralaxSecondBlurredBitmap = paralaxSecondBitmap.bitmap
        .downscale(0.2f)
        .blurBitmap(12)
        .convertToAlphaMask()
        .toBitmapWithRect()
    private val paralaxThirdBlurredBitmap = paralaxThirdBitmap.bitmap
        .downscale(0.2f)
        .blurBitmap(12)
        .convertToAlphaMask()
        .toBitmapWithRect()

    private val frostedGlassLayer: FrostedGlassLayer = FrostedGlassLayer()

    private val cardRect = RectF()
    private val cardPath = Path()
    private val cardBorderSize = 6f
    private val cardRadius = 60f

    private val shadowData = ShadowData(Color.BLACK, 25f)

    private var travelOffset = 0f

    private val frostedGlassTranslationRange = 4f

    init {
        cameraDistance = 10000f
        setWillNotDraw(false)
        gyroscopeListener.start()
    }

    override fun onViewRemoved(view: View?) {
        super.onViewRemoved(view)
        gyroscopeListener.cancel()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        cardRect.setCardBounds(this)
        cardPath.applyRoundRect(cardRect, cardRadius)
        updateHighlights()
        updateParalaxBitmapBounds()
        updateFrostedGlassLayer()
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (params is LayoutParams && params.cardBackgroundColor != null) {
            frostedGlassLayer.layers.add(ViewWithLayoutParams(child, params))
        }

        super.addView(child, index, params)
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): ConstraintLayout.LayoutParams {
        return LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return LayoutParams(p)
    }

    private fun onGyroscopeRotation(rotation: Float) {
        rotationY = rotation * CARD_MAX_ROTATION
        travelOffset = rotation
        updateHighlights()
        children.forEach { child ->
            child.translationX = getTravelOffsetInRange(frostedGlassTranslationRange)
        }
        updateFrostedGlassLayer()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCardShadow(canvas)
        canvas.clipPath(cardPath)
        drawCard(canvas)
        drawParalax(canvas)
        drawFrostedGlassLayer(canvas)
    }

    fun drawCardShadow(canvas: Canvas) {
        canvas.drawPath(cardPath, shadowData.paint)
    }

    private fun drawCard(canvas: Canvas) {
        canvas.drawBitmap(cardBackgroundBitmap, null, cardRect, null)
        canvas.drawPath(cardPath, cardBorderHighlightShader.paint)
        canvas.drawRect(cardRect, cardHighlightShader.paint)
    }

    private fun drawParalax(canvas: Canvas) {
        paralaxFirstBitmap.drawParalaxLayerByRange(canvas, 0f)
        paralaxSecondBitmap.drawParalaxLayerByRange(canvas, -8f)
        paralaxThirdBitmap.drawParalaxLayerByRange(canvas, -16f)
    }

    private fun drawBlurredParalax(canvas: Canvas) {
        paralaxFirstBlurredBitmap.drawParalaxLayerByRange(canvas, 0f)
        paralaxSecondBlurredBitmap.drawParalaxLayerByRange(canvas, -8f)
        paralaxThirdBlurredBitmap.drawParalaxLayerByRange(canvas, -16f)
    }

    private fun drawFrostedGlassLayer(canvas: Canvas) {
        frostedGlassLayer.layers.forEach {
            canvas.save()
            canvas.clipPath(it.borderPath)
            canvas.drawBitmap(cardBackgroundBitmap, null, cardRect, null)
            drawBlurredParalax(canvas)
            canvas.drawPath(it.borderPath, it.cardPaint)
            it.borderPaint?.let { borderPaint ->
                canvas.drawPath(it.borderPath, borderPaint)
                nestedViewBorderHighlightShader.paint.strokeWidth = borderPaint.strokeWidth
                canvas.drawPath(it.borderPath, nestedViewBorderHighlightShader.paint)
            }
            canvas.restore()
        }
    }

    private fun BitmapWithRect.drawParalaxLayerByRange(canvas: Canvas, range: Float) {
        if (range != 0f) {
            canvas.save()
            canvas.travelOffsetInRange(range)
            canvas.drawBitmap(bitmap, null, rect, paralaxHighlightShader.paint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, null, rect, paralaxHighlightShader.paint)
        }
    }

    private fun Canvas.travelOffsetInRange(rangeRadius: Float) {
        val pixelOffset = getTravelOffsetInRange(rangeRadius)
        translate(pixelOffset, 0f)
    }

    private fun getTravelOffsetInRange(rangeRadius: Float): Float {
        return rangeRadius * travelOffset
    }

    private fun updateHighlights() {
        val highlightOffset = getTravelOffsetInRange(width / 2f)
        cardHighlightShader.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
        cardBorderHighlightShader.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
        paralaxHighlightShader.normalizeMatrix(ScaleType.CENTER, highlightOffset)
        nestedViewBorderHighlightShader.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
    }

    private fun updateParalaxBitmapBounds() {
        paralaxFirstBitmap.normalizeBounds(ScaleType.CENTER)
        paralaxFirstBlurredBitmap.normalizeBounds(ScaleType.CENTER)
        paralaxSecondBitmap.normalizeBounds(ScaleType.CENTER)
        paralaxSecondBlurredBitmap.normalizeBounds(ScaleType.CENTER)
        paralaxThirdBitmap.normalizeBounds(ScaleType.CENTER)
        paralaxThirdBlurredBitmap.normalizeBounds(ScaleType.CENTER)
    }

    private fun updateFrostedGlassLayer() {
        frostedGlassLayer.layers.forEach {
            it.borderPath.applyRoundRect(it.view, it.cardRadius)
        }
    }

    private fun BitmapShaderWithPaint.normalizeMatrix(scaleType: ScaleType, shaderOffset: Float) {
        val scale = bitmap.calculateScale(scaleType, cardRect.width(), cardRect.height())

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            cardRect.left + (cardRect.width() - bitmap.width * scale) / 2f + shaderOffset,
            cardRect.top + (cardRect.height() - bitmap.height * scale) / 2f
        )

        shader.setLocalMatrix(matrix)
    }

    private fun BitmapWithRect.normalizeBounds(scaleType: ScaleType) {
        val scale = bitmap.calculateScale(scaleType, cardRect.width(), cardRect.height())

        rect.set(
            0f,
            0f,
            bitmap.width * scale,
            bitmap.height * scale
        )
        rect.offset((cardRect.width() - rect.width()) / 2, cardRect.top)
    }

    private fun Bitmap.calculateScale(
        scaleType: ScaleType,
        targetWidth: Float,
        targetHeight: Float
    ): Float {
        val wScale = targetWidth / this@calculateScale.width
        val hScale = targetHeight / this@calculateScale.height
        return if (scaleType == ScaleType.CENTER) {
            wScale.coerceAtLeast(hScale)
        } else {
            wScale.coerceAtMost(hScale)
        }
    }

    class LayoutParams : ConstraintLayout.LayoutParams {

        val cardBackgroundColor: Int?
        val cardBorderColor: Int?
        val strokeWidth: Float
        val cardRadius: Float

        constructor(source: ViewGroup.LayoutParams) : super(source) {
            cardBackgroundColor = null
            cardBorderColor = null
            strokeWidth = 0f
            cardRadius = 0f
        }

        constructor(source: LayoutParams) : super(source) {
            cardBackgroundColor = source.cardBackgroundColor
            cardBorderColor = source.cardBorderColor
            strokeWidth = source.strokeWidth
            cardRadius = source.cardRadius
        }

        constructor(width: Int, height: Int) : super(width, height) {
            cardBackgroundColor = null
            cardBorderColor = null
            strokeWidth = 0f
            cardRadius = 0f
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ParalaxCardView_Layout)
            cardBackgroundColor = a.getColorOrNull(
                R.styleable.ParalaxCardView_Layout_layout_cardBackgroundColor
            )
            cardBorderColor =
                a.getColorOrNull(R.styleable.ParalaxCardView_Layout_layout_cardBorderColor)
            strokeWidth = a.getDimension(R.styleable.ParalaxCardView_Layout_layout_strokeWidth, 0f)
            cardRadius = a.getDimension(R.styleable.ParalaxCardView_Layout_layout_cardRadius, 0f)
            a.recycle()
        }
    }
}
