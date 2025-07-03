package io.novafoundation.nova.common.presentation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.BottomSheeetCopierBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.copy.CopyTextLauncher
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.utils.shareText
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem

class CopierBottomSheet(
    context: Context,
    private val payload: CopyTextLauncher.Payload
) : FixedListBottomSheet<BottomSheeetCopierBinding>(
    context,
    viewConfiguration = ViewConfiguration(
        configurationBinder = BottomSheeetCopierBinding.inflate(LayoutInflater.from(context)),
        title = { configurationBinder.copierValue },
        container = { configurationBinder.copierContainer }
    )
) {

    val clipboardManager: ClipboardManager by lazy {
        FeatureUtils.getCommonApi(context).provideClipboardManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(payload.title)
        textItem(R.drawable.ic_copy_outline, payload.copyButtonName, false) {
            // TODO: We'd like to have it in mixin or view model
            clipboardManager.addToClipboard(payload.textToCopy)
            Toast.makeText(context, context.getString(R.string.common_copied), Toast.LENGTH_LONG)
                .show()
        }
        textItem(R.drawable.ic_share_outline, payload.shareButtonName, false) {
            // TODO: We'd like to have it in mixin or view model
            context.shareText(payload.textToCopy)
        }
    }
}
