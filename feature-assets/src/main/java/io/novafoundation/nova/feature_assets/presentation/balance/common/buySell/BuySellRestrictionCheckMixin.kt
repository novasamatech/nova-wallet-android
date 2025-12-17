package io.novafoundation.nova.feature_assets.presentation.balance.common.buySell

import io.novafoundation.nova.common.mixin.restrictions.RestrictionCheckMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.isMultisig
import io.novafoundation.nova.feature_account_api.domain.model.isThreshold1
import io.novafoundation.nova.feature_assets.R

class BuySellRestrictionCheckMixin(
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val actionLauncher: ActionBottomSheetLauncher,
) : RestrictionCheckMixin {

    override suspend fun isRestricted(): Boolean {
        val selectedAccount = accountUseCase.getSelectedMetaAccount()
        return selectedAccount.isMultisig() && !selectedAccount.isThreshold1()
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
            title = resourceManager.getString(R.string.multisig_sell_not_supported_title),
            subtitle = resourceManager.getString(R.string.multisig_sell_not_supported_message),
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }
}
