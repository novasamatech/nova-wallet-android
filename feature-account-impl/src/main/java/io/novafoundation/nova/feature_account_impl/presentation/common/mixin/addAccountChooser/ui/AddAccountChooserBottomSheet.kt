package io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.ui

import android.content.Context
import android.os.Bundle
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.item
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin

class AddAccountChooserBottomSheet(
    context: Context,
    private val payload: AddAccountLauncherMixin.AddAccountTypePayload
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(payload.title)

        item(R.drawable.ic_add_circle, R.string.account_create_account) {
            payload.onCreate()
        }

        item(R.drawable.ic_key, R.string.account_restore_account) {
            payload.onImport()
        }
    }
}
