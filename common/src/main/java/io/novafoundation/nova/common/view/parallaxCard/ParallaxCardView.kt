package io.novafoundation.nova.common.view.parallaxCard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getColorOrNull

private const val DEVICE_ROTATION_ANGLE_RADIUS = 16f

open class ParallaxCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ParallaxCardBitmapBaking.OnBakingPreparedCallback {

    private val gyroscopeListener = CardGyroscopeListener(context, DEVICE_ROTATION_ANGLE_RADIUS, ::onGyroscopeRotation)

    private val frostedGlassLayer: FrostedGlassLayer = FrostedGlassLayer()
    private val cardRect = RectF()
    private val cardPath = Path()
    private val cardRadius = 12.dpF(context)
    private var travelOffset = 0f
    private val parallaxTopLayerMaxTravel = 0f
    private val parallaxTMiddleLayerMaxTravel = (-15).dpF(context)
    private val parallaxBottomLayerMaxTravel = (-25).dpF(context)
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.container_card_actions_border)
        strokeWidth = 2.dpF(context)
        style = Paint.Style.STROKE
    }

    private val lruCache: BackingParallaxCardLruCache = FeatureUtils.getCommonApi(context)
        .bakingParallaxCardCache()

    private val helper = ParallaxCardBitmapBaking(context, lruCache)

    private val cardBackgroundBitmap: Bitmap

    init {
        setWillNotDraw(false)
        clipToPadding = false
        gyroscopeListener.start()

        helper.setBakingPreparedCallback(this)

        // Implement native shadow
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, width, height, cardRadius)
            }
        }

        cardBackgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_parallax_card_background)
    }

    override fun onViewRemoved(view: View?) {
        super.onViewRemoved(view)
        helper.onViewRemove()
        gyroscopeListener.cancel()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        cardRect.setCardBounds(this)
        cardPath.applyRoundRect(cardRect, cardRadius)
        if (helper.isPrepared) {
            updateLayers()
        }
    }

    override fun onBakingPrepared() {
        startFadeAnimation()
        updateLayers()
    }

    private fun updateLayers() {
        updateHighlights()
        updateParalaxBitmapBounds()
        updateFrostedGlassLayer()
    }

    private fun startFadeAnimation() {
        if (handler == null) return

        handler.post {
            helper.cardHighlightShader!!.paint.alpha = 0
            helper.cardBorderHighlightShader!!.paint.alpha = 0
            helper.parallaxHighlightShader!!.paint.alpha = 0
            helper.nestedViewBorderHighlightShader!!.paint.alpha = 0

            val fadeAnimator = ValueAnimator.ofFloat(0f, 1f)
                .setDuration(1000L)

            fadeAnimator.addUpdateListener {
                helper.cardHighlightShader!!.paint.alpha = (it.animatedFraction * 255).toInt()
                helper.cardBorderHighlightShader!!.paint.alpha = (it.animatedFraction * 255).toInt()
                helper.parallaxHighlightShader!!.paint.alpha = (it.animatedFraction * 255).toInt()
                helper.nestedViewBorderHighlightShader!!.paint.alpha = (it.animatedFraction * 255).toInt()
                invalidate()
            }

            fadeAnimator.start()
        }
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
        travelOffset = rotation

        if (helper.isPrepared) {
            updateHighlights()
            updateFrostedGlassLayer()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipPath(cardPath)
        drawCard(canvas)
        drawParalax(canvas)
        drawFrostedGlassLayer(canvas)
    }

    private fun drawCard(canvas: Canvas) {
        // Card background and border
        canvas.drawBitmap(cardBackgroundBitmap, null, cardRect, null)
        canvas.drawPath(cardPath, cardBorderPaint)

        // Highlights
        if (helper.isPrepared) {
            canvas.drawPath(cardPath, helper.cardBorderHighlightShader!!.paint)
            canvas.drawRect(cardRect, helper.cardHighlightShader!!.paint)
        }
    }

    private fun drawParalax(canvas: Canvas) {
        if (!helper.isPrepared) return

        helper.parallaxFirstBitmap!!.drawParalaxLayerByRange(canvas, parallaxTopLayerMaxTravel)
        helper.parallaxSecondBitmap!!.drawParalaxLayerByRange(canvas, parallaxTMiddleLayerMaxTravel)
        helper.parallaxThirdBitmap!!.drawParalaxLayerByRange(canvas, parallaxBottomLayerMaxTravel)
    }

    private fun drawBlurredParallax(canvas: Canvas) {
        if (!helper.isPrepared) return

        helper.parallaxFirstBlurredBitmap!!.drawParalaxLayerByRange(canvas, parallaxTopLayerMaxTravel)
        helper.parallaxSecondBlurredBitmap!!.drawParalaxLayerByRange(canvas, parallaxTMiddleLayerMaxTravel)
        helper.parallaxThirdBlurredBitmap!!.drawParalaxLayerByRange(canvas, parallaxBottomLayerMaxTravel)
    }

    private fun drawFrostedGlassLayer(canvas: Canvas) {
        frostedGlassLayer.layers.forEach {
            canvas.save()
            canvas.clipPath(it.borderPath)

            // Blurred parallax
            if (helper.isPrepared) {
                canvas.drawBitmap(cardBackgroundBitmap, null, cardRect, null)
                drawBlurredParallax(canvas)
            }

            // Nested card background and border
            canvas.drawPath(it.borderPath, it.cardPaint)
            canvas.drawPath(it.borderPath, cardBorderPaint)

            // Highlight for border
            if (helper.isPrepared) {
                canvas.drawPath(it.borderPath, helper.nestedViewBorderHighlightShader!!.paint)
            }
            canvas.restore()
        }
    }

    private fun BitmapWithRect.drawParalaxLayerByRange(canvas: Canvas, range: Float) {
        if (range != 0f) {
            canvas.save()
            canvas.travelOffsetInRange(range)
            canvas.drawBitmap(bitmap, null, rect, helper.parallaxHighlightShader!!.paint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, null, rect, helper.parallaxHighlightShader!!.paint)
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
        helper.cardHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
        helper.cardBorderHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
        helper.parallaxHighlightShader!!.normalizeMatrix(ScaleType.CENTER, highlightOffset)
        helper.nestedViewBorderHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, highlightOffset)
    }

    private fun updateParalaxBitmapBounds() {
        helper.parallaxFirstBitmap!!.normalizeBounds(ScaleType.CENTER)
        helper.parallaxFirstBlurredBitmap!!.normalizeBounds(ScaleType.CENTER)
        helper.parallaxSecondBitmap!!.normalizeBounds(ScaleType.CENTER)
        helper.parallaxSecondBlurredBitmap!!.normalizeBounds(ScaleType.CENTER)
        helper.parallaxThirdBitmap!!.normalizeBounds(ScaleType.CENTER)
        helper.parallaxThirdBlurredBitmap!!.normalizeBounds(ScaleType.CENTER)
    }

    private fun updateFrostedGlassLayer() {
        frostedGlassLayer.layers.forEach {
            it.borderPath.applyRoundRect(it.view, it.cardRadius)
        }
    }

    private fun BitmapShaderHelper.normalizeMatrix(scaleType: ScaleType, shaderOffset: Float) {
        val scale = bitmap.calculateScale(scaleType, cardRect.width(), cardRect.height())

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
        val cardRadius: Float

        constructor(source: ViewGroup.LayoutParams) : super(source) {
            cardBackgroundColor = null
            cardBorderColor = null
            cardRadius = 0f
        }

        constructor(source: LayoutParams) : super(source) {
            cardBackgroundColor = source.cardBackgroundColor
            cardBorderColor = source.cardBorderColor
            cardRadius = source.cardRadius
        }

        constructor(width: Int, height: Int) : super(width, height) {
            cardBackgroundColor = null
            cardBorderColor = null
            cardRadius = 0f
        }

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ParallaxCardView_Layout)
            cardBackgroundColor = a.getColorOrNull(
                R.styleable.ParallaxCardView_Layout_layout_cardBackgroundColor
            )
            cardBorderColor = a.getColorOrNull(R.styleable.ParallaxCardView_Layout_layout_cardBorderColor)
            cardRadius = a.getDimension(R.styleable.ParallaxCardView_Layout_layout_cardRadius, 0f)
            a.recycle()
        }
    }
}
