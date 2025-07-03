package io.novafoundation.nova.feature_assets.presentation.balance.common.multisig

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_assets.R

class MultisigRestrictionCheckMixinFactory(
    private val accountUseCase: SelectedAccountUseCase,
    private val actionLauncherFactory: ActionBottomSheetLauncherFactory,
    private val resourceManager: ResourceManager
) {

    fun create(): MultisigRestrictionCheckMixin {
        return RealMultisigRestrictionCheckMixin(
            accountUseCase = accountUseCase,
            actionLauncher = actionLauncherFactory.create(),
            resourceManager = resourceManager
        )
    }
}

interface MultisigRestrictionCheckMixin {

    val actionLauncher: ActionBottomSheetLauncher

    suspend fun isMultisig(): Boolean

    fun showWarning(title: String, message: String)
}

class RealMultisigRestrictionCheckMixin(
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    override val actionLauncher: ActionBottomSheetLauncher,
) : MultisigRestrictionCheckMixin {

    override suspend fun isMultisig(): Boolean {
        return accountUseCase.getSelectedMetaAccount().type == LightMetaAccount.Type.MULTISIG
    }

    override fun showWarning(title: String, message: String) {
        actionLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_multisig,
            title = title,
            subtitle = message,
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }
}

suspend fun MultisigRestrictionCheckMixin.isNotMultisig() = !isMultisig()

fun MultisigRestrictionCheckMixin.showNovaCardRestrictionDialog(resourceManager: ResourceManager) {
    showWarning(
        resourceManager.getString(R.string.multisig_card_not_supported_title),
        resourceManager.getString(R.string.multisig_card_not_supported_message)
    )
}
