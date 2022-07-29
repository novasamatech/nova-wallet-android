package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan

import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.permissions.setupPermissionAsker
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_scan_import_parity_signer.scanImportParitySigneContainer
import kotlinx.android.synthetic.main.fragment_scan_import_parity_signer.scanImportParitySigneScanner
import kotlinx.android.synthetic.main.fragment_scan_import_parity_signer.scanImportParitySigneViewFinder
import kotlinx.android.synthetic.main.fragment_scan_import_parity_signer.scanImportParitySigneViewTitle

class ScanImportParitySignerFragment : BaseFragment<ScanImportParitySignerViewModel>(), BarcodeCallback {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_import_parity_signer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanImportParitySigneScanner.decodeSingle(this)
        scanImportParitySigneViewFinder.setCameraPreview(scanImportParitySigneScanner)

        scanImportParitySigneViewFinder.onFinderRectChanges { finderRect ->
            positionLabel(finderRect)
        }
    }

    override fun initViews() {
        setupDecoder()
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .scanImportParitySignerComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ScanImportParitySignerViewModel) {
        viewModel.scanningAvailable.observe { scanningAvailable ->
            if (scanningAvailable) {
                scanImportParitySigneScanner.resume()
            } else {
                scanImportParitySigneScanner.pause()
            }
        }

        viewModel.resetScanningEvent.observeEvent {
            scanImportParitySigneScanner.decodeSingle(this)
        }

        setupPermissionAsker(viewModel)
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.scanningAvailable.value) {
            scanImportParitySigneScanner.resume()
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()

        viewModel.onStart()
    }

    override fun onPause() {
        super.onPause()

        scanImportParitySigneScanner.pause()

        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun barcodeResult(result: BarcodeResult) {
       viewModel.scanned(result.toString())
    }

    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}

    private fun positionLabel(finderRect: Rect) {
        val rectTop = finderRect.top
        val context = requireContext()

        // how much finderRect offsets from center of the screen + half of textView height since it is originally centered itself
        val requiredBottomMargin = scanImportParitySigneContainer.height / 2 - rectTop + scanImportParitySigneViewTitle.height / 2

        scanImportParitySigneViewTitle.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
            setMargins(16.dp(context), 0, 16.dp(context), requiredBottomMargin + 24.dp(context))
        }
        scanImportParitySigneViewTitle.makeVisible()
    }

    private fun setupDecoder() {
        val charSet = Charsets.UTF_8.name()
        val formats = listOf(BarcodeFormat.QR_CODE)
        val hints = emptyMap<DecodeHintType, Any>()
        val inverted = false

        scanImportParitySigneScanner.decoderFactory = DefaultDecoderFactory(
            formats,
            hints,
            charSet,
            inverted
        )
        scanImportParitySigneScanner.framingRect
    }
}
