package io.novafoundation.nova.common.utils.systemCall

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity

class WebViewFilePickerSystemCallFactory {
    fun create(fileChooserParams: WebChromeClient.FileChooserParams?): WebViewFilePickerSystemCall {
        return WebViewFilePickerSystemCall(fileChooserParams)
    }
}

class WebViewFilePickerSystemCall(
    private val fileChooserParams: WebChromeClient.FileChooserParams?
) : SystemCall<Uri> {

    companion object {

        private const val REQUEST_CODE = 301
    }

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val chooserIntent = if (fileChooserParams == null) {
            val selectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            selectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            selectionIntent.type = "*/*"
            Intent.createChooser(selectionIntent, null)
        } else {
            fileChooserParams.createIntent()
        }

        return SystemCall.Request(chooserIntent, REQUEST_CODE)
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Uri> {
        if (resultCode != RESULT_OK) return Result.failure(UnsupportedOperationException())

        val data = intent?.data

        return if (data != null) {
            Result.success(data)
        } else {
            Result.failure(UnsupportedOperationException())
        }
    }
}
