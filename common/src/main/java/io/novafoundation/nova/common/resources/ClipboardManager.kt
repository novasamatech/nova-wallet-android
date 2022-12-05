package io.novafoundation.nova.common.resources

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.content.ClipboardManager as NativeClipboardManager

private const val DEFAULT_LABEL = "nova"

class ClipboardManager(
    private val clipboardManager: NativeClipboardManager
) {

    fun observePrimaryClip(): Flow<String?> = callbackFlow {
        send(getTextOrNull())

        val listener = NativeClipboardManager.OnPrimaryClipChangedListener {
            trySend(getTextOrNull())
        }

        clipboardManager.addPrimaryClipChangedListener(listener)

        awaitClose {
            clipboardManager.removePrimaryClipChangedListener(listener)
        }
    }

    fun getTextOrNull(): String? {
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

    fun getAny(): String? {
        return with(clipboardManager) {
            if (hasPrimaryClip()) {
                val item: ClipData.Item = primaryClip!!.getItemAt(0)

                item.text.toString()
            } else {
                null
            }
        }
    }

    fun addToClipboard(text: String, label: String = DEFAULT_LABEL) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
}
