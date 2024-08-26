package io.novafoundation.nova.app.root.presentation

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import io.novafoundation.nova.common.utils.systemCall.SystemCall
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class MercurioCardWebViewController(
    val webView: WebView,
    val systemCallExecutor: SystemCallExecutor,
    val coroutineScope: CoroutineScope
) {

    init {
        ActivityCompat.requestPermissions(webView.context as Activity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), 100)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportMultipleWindows(true)
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        val page = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Mercuryo Widget</title>
        </head>
        <body>
            <div id="widget-container"></div>
        
            <script src="https://widget.mercuryo.io/embed.2.0.js"></script>
        </body>
        </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://exchange.mercuryo.io", page, "text/html", "UTF-8", null)

        // webView.loadUrl("https://exchange.mercuryo.io")

        // Inject the JavaScript after the page has fully loaded
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                // Inject the widget script
                webView.evaluateJavascript(
                    """
                    mercuryoWidget.run({ 
                        widgetId: '4ce98182-ed76-4933-ba1b-b85e4a51d75a',
                        host: document.getElementById('widget-container'),
                        type: 'sell',
                        currency: 'DOT',
                        fiatCurrency: 'EUR',
                        paymentMethod: 'fiat_card_open',
                        width: '100%',
                        height: window.innerHeight,
                        hideRefundAddress: true,
                        refundAddress: '12gkMmfdKq7aEnAXwb2NSxh9vLqKifoCaoafLrR6E6swZRmc',
                        fixPaymentMethod: true,
                        showSpendCardDetails: true,
                        onStatusChange: data => {
                            console.log('onStatusChange:', data);
                            Android.onStatusChange(JSON.stringify(data));
                        },
                        onSellTransferEnabled: data => {
                            console.log('onSellTransferEnabled:', data);
                            Android.onSellTransferEnabled(JSON.stringify(data));
                        }
                    });
                    """.trimIndent(),
                    null
                )
            }
        }

        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }


            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                coroutineScope.launch {
                    val result = systemCallExecutor.executeSystemCall(FilePickerSystemCall())
                    result.onSuccess { filePathCallback.onReceiveValue(arrayOf(it)) }
                }
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d("MercurioCardWebViewController onConsoleMessage", consoleMessage.message())

                return true
            }
        }
    }
}

class WebAppInterface {
    @JavascriptInterface
    fun onStatusChange(data: String) {
        Log.d("MercurioCardWebViewController onStatusChange", data)
    }

    @JavascriptInterface
    fun onSellTransferEnabled(data: String) {
        Log.d("MercurioCardWebViewController onSellTransferEnabled", data)
    }
}

private class FilePickerSystemCall : SystemCall<Uri> {

    companion object {

        private const val REQUEST_CODE = 301
    }

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val pickIntent = Intent(Intent.ACTION_PICK)
        pickIntent.setType("image/*|application/*")

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (captureIntent.resolveActivity(activity.packageManager) != null) {
            val photoUri: Uri = getPhotoUri(activity)
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        val chooserIntent = Intent.createChooser(pickIntent, "Выберите действие")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))

        return SystemCall.Request(chooserIntent, REQUEST_CODE)
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Uri> {
        val data = intent?.data
        return if (resultCode == RESULT_OK && data != null) {
            Result.success(data)
        } else {
            Result.failure(UnsupportedOperationException())
        }
    }

    private fun getPhotoUri(activity: AppCompatActivity): Uri {
        val fileName = "photo_" + System.currentTimeMillis() + ".jpg"
        val storageDir: File = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val photoFile = File(storageDir, fileName)

        val authority = "${activity.packageName}.provider"
        return FileProvider.getUriForFile(activity, authority, photoFile)
    }
}
