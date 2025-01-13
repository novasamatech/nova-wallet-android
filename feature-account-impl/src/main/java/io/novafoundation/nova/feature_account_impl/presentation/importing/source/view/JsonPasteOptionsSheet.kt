package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_impl.R

class JsonPasteOptionsSheet(
    context: Context,
    val onPaste: () -> Unit,
    val onOpenFile: () -> Unit
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.recovery_json)

        textItem(iconRes = R.drawable.ic_copy_outline, titleRes = R.string.import_json_paste) {
            onPaste()
        }

        textItem(iconRes = R.drawable.ic_json_file_upload_outline, titleRes = R.string.recover_json_upload) {
            onOpenFile()
        }
    }
}
