package io.novafoundation.nova.feature_assets.presentation.novacard.common

import io.novafoundation.nova.common.mixin.restrictions.RestrictionCheckMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.isThreshold1
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private const val NOVA_CARD_AVAILABLE_CHAIN_ID = ChainGeneses.POLKADOT

class NovaCardRestrictionCheckMixin(
    private val accountUseCase: SelectedAccountUseCase,
    private val resourceManager: ResourceManager,
    private val actionLauncher: ActionBottomSheetLauncher,
    private val chainRegistry: ChainRegistry
) : RestrictionCheckMixin {

    override suspend fun isRestricted(): Boolean {
        val selectedAccount = accountUseCase.getSelectedMetaAccount()
        val availableChain = chainRegistry.getChain(NOVA_CARD_AVAILABLE_CHAIN_ID)

        return isRestrictedByMultisig(selectedAccount, availableChain) || isRestrictedByProxied(selectedAccount, availableChain)
    }

    override suspend fun checkRestrictionAndDo(action: () -> Unit) {
        val selectedAccount = accountUseCase.getSelectedMetaAccount()
        val availableChain = chainRegistry.getChain(NOVA_CARD_AVAILABLE_CHAIN_ID)

        when {
            isRestrictedByMultisig(selectedAccount, availableChain) -> showMultisigWarning()
            isRestrictedByProxied(selectedAccount, availableChain) -> showProxiedWarning()
            else -> action()
        }
    }

    private fun isRestrictedByMultisig(metaAccount: MetaAccount, availableChain: Chain): Boolean {
        if (metaAccount !is MultisigMetaAccount) return false

        if (metaAccount.isThreshold1() && metaAccount.hasAccountIn(availableChain)) return false

        return true
    }

    private fun isRestrictedByProxied(metaAccount: MetaAccount, availableChain: Chain): Boolean {
        if (metaAccount !is ProxiedMetaAccount) return false

        return !metaAccount.hasAccountIn(availableChain)
    }

    private fun showMultisigWarning() {
        actionLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_multisig,
            title = resourceManager.getString(R.string.multisig_card_not_supported_title),
            subtitle = resourceManager.getString(R.string.multisig_card_not_supported_message),
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }

    private fun showProxiedWarning() {
        actionLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_proxy,
            title = resourceManager.getString(R.string.proxied_card_not_supported_title),
            subtitle = resourceManager.getString(R.string.proxied_card_not_supported_message),
            actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_ok_back)),
            neutralButtonPreferences = null
        )
    }
}
