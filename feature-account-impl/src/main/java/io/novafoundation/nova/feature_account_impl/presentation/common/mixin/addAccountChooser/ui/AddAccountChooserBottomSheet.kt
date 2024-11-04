package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.ui

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.databinding.BottomSheeetFixedListBinding
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textItem
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin

class AddAccountChooserBottomSheet(
    context: Context,
    private val payload: AddAccountLauncherMixin.AddAccountTypePayload
) : FixedListBottomSheet<BottomSheeetFixedListBinding>(context, viewConfiguration = ViewConfiguration.default(context)) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.title)

        textItem(R.drawable.ic_add_circle_outline, R.string.account_create_account) {
            payload.onCreate()
        }

        textItem(R.drawable.ic_import, R.string.account_export_existing) {
            payload.onImport()
        }
    }
}
