package io.novafoundation.nova.feature_account_impl.presentation.importing.source.view

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.feature_account_impl.R

class JsonPasteOptionsSheet(
    context: Context,
    val onPaste: () -> Unit,
    val onOpenFile: () -> Unit
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.recovery_json)

        item(icon = R.drawable.ic_copy_24, titleRes = R.string.import_json_paste) {
            onPaste()
        }

        item(icon = R.drawable.ic_file_upload, titleRes = R.string.recover_json_upload) {
            onOpenFile()
        }
    }
}
