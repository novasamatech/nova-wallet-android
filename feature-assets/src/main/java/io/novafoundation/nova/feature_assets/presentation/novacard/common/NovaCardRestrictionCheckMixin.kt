package io.novafoundation.nova.feature_assets.presentation.novacard.common

import io.novafoundation.nova.common.mixin.restrictions.RestrictionCheckMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asMultisig
import io.novafoundation.nova.feature_account_api.domain.model.asProxied
import io.novafoundation.nova.feature_account_api.domain.model.isMultisig
import io.novafoundation.nova.feature_account_api.domain.model.isProxied
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

        return when (selectedAccount.type) {
            LightMetaAccount.Type.PROXIED -> selectedAccount.asProxied().isRestricted(availableChain)
            LightMetaAccount.Type.MULTISIG -> selectedAccount.asMultisig().isRestricted(availableChain)

            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.POLKADOT_VAULT -> false
        }
    }

    override suspend fun checkRestrictionAndDo(action: () -> Unit) {
        val selectedAccount = accountUseCase.getSelectedMetaAccount()
        val availableChain = chainRegistry.getChain(NOVA_CARD_AVAILABLE_CHAIN_ID)

        when {
            selectedAccount.isProxied() && selectedAccount.isRestricted(availableChain) -> showProxiedWarning()
            selectedAccount.isMultisig() && selectedAccount.isRestricted(availableChain) -> showMultisigWarning()

            else -> action()
        }
    }

    private fun MultisigMetaAccount.isRestricted(availableChain: Chain): Boolean {
        val isAllowed = isThreshold1() && hasAccountIn(availableChain)

        return !isAllowed
    }

    private fun ProxiedMetaAccount.isRestricted(availableChain: Chain): Boolean {
        val isAllowed = hasAccountIn(availableChain)
        return !isAllowed
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
