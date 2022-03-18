package io.novafoundation.nova.common.utils.systemCall

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class ScanQrCodeCall : SystemCall<String> {

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val integrator = IntentIntegrator(activity).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            setPrompt("")
            setBeepEnabled(false)
        }

        return SystemCall.Request(
            intent = integrator.createScanIntent(),
            requestCode = IntentIntegrator.REQUEST_CODE
        )
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<String> {
        val qrContent = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)?.contents

        return when {
            resultCode == Activity.RESULT_CANCELED -> Result.failure(SystemCall.Failure.Cancelled())
            qrContent == null -> Result.failure(SystemCall.Failure.Unknown())
            else -> Result.success(qrContent)
        }
    }
}
