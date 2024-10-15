package io.novafoundation.nova.common.presentation.scan

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewScanBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.useAttributes

class ScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    val binder = ViewScanBinding.inflate(inflater(), this)

    init {
        setupDecoder()

        binder.viewScanViewFinder.setCameraPreview(binder.viewScanScanner)

        binder.viewScanViewFinder.onFinderRectChanges {
            positionLabels(it)
        }

        attrs?.let(::applyAttributes)
    }

    val subtitle: TextView
        get() = binder.viewScanSubtitle

    fun resume() {
        binder.viewScanScanner.resume()
    }

    fun pause() {
        binder.viewScanScanner.pause()
    }

    inline fun startDecoding(crossinline onScanned: (String) -> Unit) {
        binder.viewScanScanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                onScanned(result.toString())
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!changed) return

        binder.viewScanViewFinder.framingRect?.let {
            positionLabels(it)
        }
    }

    fun setTitle(title: String) {
        binder.viewScanTitle.text = title
    }

    fun setSubtitle(subtitle: String) {
        binder.viewScanSubtitle.text = subtitle
    }

    private fun setupDecoder() {
        binder.viewScanScanner.decoderFactory = AlternatingDecoderFactory(
            decodeFormats = listOf(BarcodeFormat.QR_CODE),
            hints = null,
            characterSet = null,
        )
    }

    private fun positionLabels(finderRect: Rect) {
        binder.viewScanTitle.doIfHasText { positionTitle(finderRect) }
        binder.viewScanSubtitle.doIfHasText { positionSubTitle(finderRect) }
    }

    private inline fun TextView.doIfHasText(action: () -> Unit) {
        if (text.isNotEmpty()) action()
    }

    private fun positionTitle(finderRect: Rect) {
        val rectTop = finderRect.top

        // how much finderRect offsets from center of the screen + half of textView height since it is originally centered itself
        val requiredBottomMargin = height / 2 - rectTop + binder.viewScanTitle.height / 2

        binder.viewScanTitle.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(16.dp, 0, 16.dp, requiredBottomMargin + 24.dp)
        }
        binder.viewScanTitle.makeVisible()
    }

    private fun positionSubTitle(finderRect: Rect) {
        val rectBottom = finderRect.bottom

        // how much finderRect offsets from center of the screen + half of textView height since it is originally centered itself
        val requiredTopMargin = rectBottom - height / 2 + binder.viewScanSubtitle.height / 2

        binder.viewScanSubtitle.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(16.dp, requiredTopMargin + 24.dp, 16.dp, 0)
        }
        binder.viewScanSubtitle.makeVisible()
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ScanView) { typedArray ->
        val title = typedArray.getString(R.styleable.ScanView_title)
        title?.let(::setTitle)

        val subTitle = typedArray.getString(R.styleable.ScanView_subTitle)
        subTitle?.let(::setSubtitle)
    }
}
