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
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.android.synthetic.main.view_scan.view.viewScanScanner
import kotlinx.android.synthetic.main.view_scan.view.viewScanSubtitle
import kotlinx.android.synthetic.main.view_scan.view.viewScanTitle
import kotlinx.android.synthetic.main.view_scan.view.viewScanViewFinder

class ScanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_scan, this)

        setupDecoder()

        viewScanViewFinder.setCameraPreview(viewScanScanner)

        viewScanViewFinder.onFinderRectChanges {
            positionLabels(it)
        }

        attrs?.let(::applyAttributes)
    }

    val subtitle: TextView
        get() = viewScanSubtitle

    fun resume() {
        viewScanScanner.resume()
    }

    fun pause() {
        viewScanScanner.pause()
    }

    inline fun startDecoding(crossinline onScanned: (String) -> Unit) {
        viewScanScanner.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                onScanned(result.toString())
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (!changed) return

        viewScanViewFinder.framingRect?.let {
            positionLabels(it)
        }
    }

    fun setTitle(title: String) {
        viewScanTitle.text = title
    }

    fun setSubtitle(subtitle: String) {
        viewScanSubtitle.text = subtitle
    }

    private fun setupDecoder() {
        viewScanScanner.decoderFactory = AlternatingDecoderFactory(
            decodeFormats = listOf(BarcodeFormat.QR_CODE),
            hints = null,
            characterSet = null,
        )
    }

    private fun positionLabels(finderRect: Rect) {
        viewScanTitle.doIfHasText { positionTitle(finderRect) }
        viewScanSubtitle.doIfHasText { positionSubTitle(finderRect) }
    }

    private inline fun TextView.doIfHasText(action: () -> Unit) {
        if (text.isNotEmpty()) action()
    }

    private fun positionTitle(finderRect: Rect) {
        val rectTop = finderRect.top

        // how much finderRect offsets from center of the screen + half of textView height since it is originally centered itself
        val requiredBottomMargin = height / 2 - rectTop + viewScanTitle.height / 2

        viewScanTitle.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(16.dp, 0, 16.dp, requiredBottomMargin + 24.dp)
        }
        viewScanTitle.makeVisible()
    }

    private fun positionSubTitle(finderRect: Rect) {
        val rectBottom = finderRect.bottom

        // how much finderRect offsets from center of the screen + half of textView height since it is originally centered itself
        val requiredTopMargin = rectBottom - height / 2 + viewScanSubtitle.height / 2

        viewScanSubtitle.layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(16.dp, requiredTopMargin + 24.dp, 16.dp, 0)
        }
        viewScanSubtitle.makeVisible()
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ScanView) { typedArray ->
        val title = typedArray.getString(R.styleable.ScanView_title)
        title?.let(::setTitle)

        val subTitle = typedArray.getString(R.styleable.ScanView_subTitle)
        subTitle?.let(::setSubtitle)
    }
}
