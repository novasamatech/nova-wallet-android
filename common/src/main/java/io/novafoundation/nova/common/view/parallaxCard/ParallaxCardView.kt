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
import io.novafoundation.nova.common.view.parallaxCard.gyroscope.CardGyroscopeListener

private const val DEVICE_ROTATION_ANGLE_RADIUS = 16f

open class ParallaxCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ParallaxCardBitmapBaking.OnBakingPreparedCallback {

    private val gyroscopeListener = CardGyroscopeListener(
        context,
        TravelVector(DEVICE_ROTATION_ANGLE_RADIUS, DEVICE_ROTATION_ANGLE_RADIUS),
        ::onGyroscopeRotation
    )

    private val frostedGlassLayer: FrostedGlassLayer = FrostedGlassLayer()
    private val cardRect = RectF()
    private val cardPath = Path()
    private val cardRadius = 12.dpF(context)
    private var travelOffset = TravelVector(0f, 0f)

    // We use padding to support vertical highlight animation.
    private var highlightPadding = 100.dpF(context)
    private var verticalParallaxHighlightPadding = 100.dpF(context)

    private var parallaxHighlihtMaxTravel = TravelVector(0f, 0f)
    private var cardHighlightMaxTravel = TravelVector(0f, 0f)
    private val parallaxTopLayerMaxTravel = TravelVector(-7f, -3f)
    private val parallaxMiddleLayerMaxTravel = TravelVector((15).dpF(context), (8).dpF(context))
    private val parallaxBottomLayerMaxTravel = TravelVector((25).dpF(context), (19).dpF(context))
    private val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.container_card_actions_border)
        strokeWidth = 2.dpF(context)
        style = Paint.Style.STROKE
    }

    private val lruCache: BackingParallaxCardLruCache = FeatureUtils.getCommonApi(context).bakingParallaxCardCache()

    private val helper = ParallaxCardBitmapBaking(context, lruCache)

    private val cardBackgroundBitmap: Bitmap

    init {
        clipToPadding = false

        // Implement native shadow
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, width, height, cardRadius)
            }
        }

        cardBackgroundBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_parallax_card_background)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        helper.setBakingPreparedCallback(this)

        setWillNotDraw(false)

        postDelayed({
            gyroscopeListener.start()
        }, 300) //Added small delay to avoid wrong parallax initial position
    }

    override fun onViewRemoved(view: View?) {
        super.onViewRemoved(view)
        helper.onViewRemove()
        gyroscopeListener.cancel()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        cardHighlightMaxTravel.set(-width.toFloat() / 2, -highlightPadding)
        parallaxHighlihtMaxTravel.set(-width.toFloat() / 2, -height.toFloat() / 2)

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
        updateParallaxBitmapBounds()
        updateFrostedGlassLayer()
    }

    private fun startFadeAnimation() {
        if (handler == null) return

        handler.post {
            helper.cardHighlightShader!!.setAlpha(0f)
            helper.cardBorderHighlightShader!!.setAlpha(0f)
            helper.parallaxHighlightShader!!.setAlpha(0f)
            helper.nestedViewBorderHighlightShader!!.setAlpha(0f)

            val fadeAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(1000L)

            fadeAnimator.addUpdateListener {
                helper.cardHighlightShader!!.setAlpha(it.animatedFraction)
                helper.cardBorderHighlightShader!!.setAlpha(it.animatedFraction)
                helper.parallaxHighlightShader!!.setAlpha(it.animatedFraction)
                helper.nestedViewBorderHighlightShader!!.setAlpha(it.animatedFraction)
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

    private fun onGyroscopeRotation(rotation: TravelVector) {
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
        drawParallax(canvas)
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

    private fun drawParallax(canvas: Canvas) {
        if (!helper.isPrepared) return

        helper.parallaxThirdBitmap!!.drawParallaxLayerByRange(canvas, 0.7f, parallaxBottomLayerMaxTravel)
        helper.parallaxSecondBitmap!!.drawParallaxLayerByRange(canvas, 0.8f, parallaxMiddleLayerMaxTravel)
        helper.parallaxFirstBitmap!!.drawParallaxLayerByRange(canvas, 1f, parallaxTopLayerMaxTravel)
    }

    private fun drawBlurredParallax(canvas: Canvas) {
        if (!helper.isPrepared) return

        helper.parallaxThirdBlurredBitmap!!.drawParallaxLayerByRange(canvas, 0.7f, parallaxBottomLayerMaxTravel)
        helper.parallaxSecondBlurredBitmap!!.drawParallaxLayerByRange(canvas, 0.8f, parallaxMiddleLayerMaxTravel)
        helper.parallaxFirstBlurredBitmap!!.drawParallaxLayerByRange(canvas, 1f, parallaxTopLayerMaxTravel)
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

    private fun BitmapWithRect.drawParallaxLayerByRange(canvas: Canvas, alpha: Float, range: TravelVector) {
        canvas.save()
        helper.parallaxHighlightShader!!.paint.alpha = (alpha * 255).toInt()
        canvas.travelOffsetInRange(range)
        canvas.drawBitmap(bitmap, null, rect, helper.parallaxHighlightShader!!.paint)
        canvas.restore()
    }

    private fun Canvas.travelOffsetInRange(travelVector: TravelVector) {
        val pixelOffset = getTravelOffsetInRange(travelVector)
        translate(pixelOffset.x, pixelOffset.y)
    }

    private fun getTravelOffsetInRange(rangeRadius: TravelVector): TravelVector {
        return travelOffset * rangeRadius
    }

    private fun updateHighlights() {
        val cardHighlightOffset = getTravelOffsetInRange(cardHighlightMaxTravel)
        val parallaxHighlightOffset = getTravelOffsetInRange(parallaxHighlihtMaxTravel)
        helper.cardHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, cardHighlightOffset, -highlightPadding, -highlightPadding)
        helper.cardBorderHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, cardHighlightOffset, -highlightPadding, -highlightPadding)
        helper.nestedViewBorderHighlightShader!!.normalizeMatrix(ScaleType.CENTER_INSIDE, cardHighlightOffset, -highlightPadding, -highlightPadding)
        helper.parallaxHighlightShader!!.normalizeMatrix(ScaleType.CENTER, parallaxHighlightOffset, -verticalParallaxHighlightPadding)
    }

    private fun updateParallaxBitmapBounds() {
        val paddingOffset = 19.dpF(context)
        helper.parallaxFirstBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
        helper.parallaxFirstBlurredBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
        helper.parallaxSecondBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
        helper.parallaxSecondBlurredBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
        helper.parallaxThirdBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
        helper.parallaxThirdBlurredBitmap!!.normalizeBounds(ScaleType.CENTER, paddingVertical = -paddingOffset, paddingHorizontal = 0f)
    }

    private fun updateFrostedGlassLayer() {
        frostedGlassLayer.layers.forEach {
            it.borderPath.applyRoundRect(it.view, it.cardRadius)
        }
    }

    private fun BitmapShaderHelper.normalizeMatrix(
        scaleType: ScaleType,
        shaderOffset: TravelVector,
        paddingVertical: Float = 0f,
        paddingHorizontal: Float = 0f
    ) {
        val scale = bitmap.calculateScale(scaleType, cardRect.width(), cardRect.height(), paddingVertical, paddingHorizontal)

        matrix.setScale(scale, scale)
        matrix.postTranslate(
            cardRect.left + (cardRect.width() - bitmap.width * scale) / 2f + shaderOffset.x,
            cardRect.top + (cardRect.height() - bitmap.height * scale) / 2f + shaderOffset.y
        )

        shader.setLocalMatrix(matrix)
    }

    private fun BitmapWithRect.normalizeBounds(
        scaleType: ScaleType,
        paddingVertical: Float = 0f,
        paddingHorizontal: Float = 0f
    ) {
        val scale = bitmap.calculateScale(scaleType, cardRect.width(), cardRect.height(), paddingVertical, paddingHorizontal)

        rect.set(
            0f,
            0f,
            bitmap.width * scale,
            bitmap.height * scale
        )
        rect.offset((cardRect.width() - rect.width()) / 2, (cardRect.height() - rect.height()) / 2)
    }

    private fun Bitmap.calculateScale(
        scaleType: ScaleType,
        targetWidth: Float,
        targetHeight: Float,
        paddingVertical: Float,
        paddingHorizontal: Float
    ): Float {
        val wScale = (targetWidth - paddingHorizontal * 2) / this@calculateScale.width
        val hScale = (targetHeight - paddingVertical * 2) / this@calculateScale.height
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
