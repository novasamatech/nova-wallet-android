package io.novafoundation.nova.feature_account_impl.presentation.account.common.listing

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.R

fun mapMetaAccountTypeToUi(type: LightMetaAccount.Type, resourceManager: ResourceManager): ChipLabelModel? = when (type) {
    LightMetaAccount.Type.SECRETS -> null
    LightMetaAccount.Type.WATCH_ONLY -> ChipLabelModel(
        iconRes = R.drawable.ic_watch,
        title = resourceManager.getString(R.string.account_watch_only)
    )
    LightMetaAccount.Type.PARITY_SIGNER -> ChipLabelModel(
        iconRes = R.drawable.ic_parity_signer,
        title = resourceManager.getString(R.string.account_parity_signer)
    )
    LightMetaAccount.Type.LEDGER -> ChipLabelModel(
        iconRes = R.drawable.ic_ledger,
        title = resourceManager.getString(R.string.common_ledger)
    )
}
