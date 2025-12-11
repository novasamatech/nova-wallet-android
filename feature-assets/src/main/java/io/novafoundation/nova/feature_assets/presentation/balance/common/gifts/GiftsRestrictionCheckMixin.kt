package io.novafoundation.nova.feature_assets.presentation.balance.common.gifts

import io.novafoundation.nova.common.mixin.restrictions.RestrictionCheckMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_gift_api.domain.GiftsAccountSupportedUseCase
import io.novafoundation.nova.feature_gift_api.domain.GiftsSupportedState

class GiftsRestrictionCheckMixin(
    private val accountSupportedUseCase: GiftsAccountSupportedUseCase,
    private val resourceManager: ResourceManager,
    private val actionLauncher: ActionBottomSheetLauncher,
) : RestrictionCheckMixin {

    override suspend fun isRestricted(): Boolean {
        return accountSupportedUseCase.supportedState() != GiftsSupportedState.SUPPORTED
    }

    override suspend fun checkRestrictionAndDo(action: () -> Unit) {
        when {
            isRestricted() -> showMultisigWarning()
            else -> action()
        }
    }

    private fun showMultisigWarning() {
        actionLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_multisig,
            title = resourceManager.getString(R.string.multisig_gifts_not_supported_title),
            subtitle = resourceManager.getString(R.string.multisig_gifts_not_supported_message),
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }
}
