package io.novafoundation.nova.common.utils.share

import android.content.Intent
import android.net.Uri
import io.novafoundation.nova.common.base.BaseFragment

data class ImageWithTextSharing(
    val fileUri: Uri,
    val shareMessage: String
)

fun BaseFragment<*, *>.shareImageWithText(sharingData: ImageWithTextSharing, chooserTitle: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, sharingData.fileUri)
        putExtra(Intent.EXTRA_TEXT, sharingData.shareMessage)
    }

    startActivity(Intent.createChooser(intent, chooserTitle))
}
