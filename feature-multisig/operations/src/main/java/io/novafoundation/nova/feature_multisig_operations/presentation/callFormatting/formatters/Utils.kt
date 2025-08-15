package io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.formatters

import io.novafoundation.nova.common.data.model.AssetIconMode
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain


fun AssetIconProvider.multisigFormatAssetIcon(chain: Chain) = getAssetIconOrFallback(chain.utilityAsset, AssetIconMode.WHITE)
