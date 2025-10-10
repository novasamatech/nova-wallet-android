package io.novafoundation.nova.feature_wallet_api.presentation.formatters

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface AssetModelFormatter {
    suspend fun formatAsset(
        chainAsset: Chain.Asset,
        balance: MaskableModel<Balance>,
        @StringRes patternId: Int? = R.string.common_available_format
    ): AssetModel
}
