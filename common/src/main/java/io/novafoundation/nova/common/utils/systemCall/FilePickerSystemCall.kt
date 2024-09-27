package io.novafoundation.nova.common.utils.systemCall

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import io.novafoundation.nova.common.interfaces.FileProvider

class FilePickerSystemCall(
    private val fileProvider: FileProvider
) : SystemCall<Uri> {

    companion object {

        private const val REQUEST_CODE = 301
    }

    override fun createRequest(activity: AppCompatActivity): SystemCall.Request {
        val pickIntent = Intent(Intent.ACTION_PICK)
        pickIntent.setType("image/*|application/*")

        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (captureIntent.resolveActivity(activity.packageManager) != null) {
            val photoUri: Uri = getPhotoUri()
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        val chooserIntent = Intent.createChooser(pickIntent, "Выберите действие")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(captureIntent))

        return SystemCall.Request(chooserIntent, REQUEST_CODE)
    }

    override fun parseResult(requestCode: Int, resultCode: Int, intent: Intent?): Result<Uri> {
        if (resultCode != RESULT_OK) return Result.failure(UnsupportedOperationException())

        val data = intent?.data

        return if (data != null) {
            Result.success(data)
        } else {
            Result.success(getPhotoUri())
        }
    }

    private fun getPhotoUri(): Uri {
        val tempFile = fileProvider.generateTempFile("temporaryPhoto.png")

        return fileProvider.uriOf(tempFile)
    }
}
