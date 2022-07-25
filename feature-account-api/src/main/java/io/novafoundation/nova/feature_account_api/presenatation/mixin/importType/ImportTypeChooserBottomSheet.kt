package io.novafoundation.nova.feature_account_api.presenatation.mixin.importType

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType
import kotlinx.android.synthetic.main.item_source_type.view.itemSourceTypeIcon
import kotlinx.android.synthetic.main.item_source_type.view.itemSourceTypeSubtitle
import kotlinx.android.synthetic.main.item_source_type.view.itemSourceTypeTitle

class ImportTypeChooserBottomSheet(
    context: Context,
    private val onChosen: (SecretType) -> Unit,
    private val allowedSources: Set<SecretType>
) : FixedListBottomSheet(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.account_select_secret_source)

        item(
            type = SecretType.MNEMONIC,
            title = R.string.recovery_passphrase,
            subtitle = R.string.account_mnmonic_length_variants,
            icon = R.drawable.ic_text_phrase
        )

        item(
            type = SecretType.SEED,
            title = R.string.recovery_raw_seed,
            subtitle = R.string.account_private_key,
            icon = R.drawable.ic_text_0x
        )

        item(
            type = SecretType.JSON,
            title = R.string.recovery_json,
            subtitle = R.string.account_json_file,
            icon = R.drawable.ic_document
        )
    }

    private fun item(
        type: SecretType,
        @StringRes title: Int,
        @StringRes subtitle: Int,
        @DrawableRes icon: Int
    ) {
        if (type !in allowedSources) return

        item(R.layout.item_source_type) {
            it.itemSourceTypeIcon.setImageResource(icon)
            it.itemSourceTypeTitle.setText(title)
            it.itemSourceTypeSubtitle.setText(subtitle)

            it.setDismissingClickListener { onChosen(type) }
        }
    }
}
