package io.novafoundation.nova.feature_account_api.presenatation.mixin.importType

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.FixedListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.fixed.textWithDescriptionItem
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.account.add.SecretType

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
            icon = R.drawable.ic_mnemonic_phrase
        )

        item(
            type = SecretType.SEED,
            title = R.string.recovery_raw_seed,
            subtitle = R.string.common_hexadecimal_string,
            icon = R.drawable.ic_raw_seed
        )

        item(
            type = SecretType.JSON,
            title = R.string.recovery_json,
            subtitle = R.string.account_json_file,
            icon = R.drawable.ic_file_outline
        )
    }

    private fun item(
        type: SecretType,
        @StringRes title: Int,
        @StringRes subtitle: Int,
        @DrawableRes icon: Int
    ) {
        if (type !in allowedSources) return

        textWithDescriptionItem(
            titleRes = title,
            descriptionRes = subtitle,
            iconRes = icon,
        ) {
            onChosen(type)
        }
    }
}
