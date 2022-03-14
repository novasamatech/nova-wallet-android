package io.novafoundation.nova.common.resources

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import io.novafoundation.nova.common.utils.safeOffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.content.ClipboardManager as NativeClipboardManager

private const val DEFAULT_LABEL = "nova"

class ClipboardManager(
    private val clipboardManager: NativeClipboardManager
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePrimaryClip(): Flow<String?> = callbackFlow {
        send(getFromClipboard())

        val listener = NativeClipboardManager.OnPrimaryClipChangedListener {
            safeOffer(getFromClipboard())
        }

        clipboardManager.addPrimaryClipChangedListener(listener)

        awaitClose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    fun getFromClipboard(): String? {
        return with(clipboardManager) {
            if (!hasPrimaryClip()) {
                null
            } else if (!primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                null
            } else {
                val item: ClipData.Item = primaryClip!!.getItemAt(0)

                item.text.toString()
            }
        }
    }

    fun addToClipboard(text: String, label: String = DEFAULT_LABEL) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
}
