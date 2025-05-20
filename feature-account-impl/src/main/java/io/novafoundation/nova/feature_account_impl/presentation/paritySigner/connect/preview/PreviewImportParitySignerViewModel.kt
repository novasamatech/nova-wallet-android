package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.preview

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.preview.BaseChainAccountsPreviewViewModel
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.formatWithPolkadotVaultLabel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.preview.PreviewImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class PreviewImportParitySignerViewModel(
    private val interactor: PreviewImportParitySignerInteractor,
    private val accountRouter: AccountRouter,
    private val iconGenerator: AddressIconGenerator,
    private val payload: ParitySignerAccountPayload,
    private val externalActions: ExternalActions.Presentation,
    private val chainRegistry: ChainRegistry,
    private val resourceManager: ResourceManager,
) : BaseChainAccountsPreviewViewModel(iconGenerator, externalActions, chainRegistry, accountRouter) {

    override val subtitle: String = resourceManager.formatWithPolkadotVaultLabel(R.string.account_parity_signer_import_preview_description, payload.variant)

    override val chainAccountProjections = flowOf { interactor.deriveSubstrateChainAccounts(payload.accountId) }
        .mapList { mapChainAccountPreviewToUi(it) }
        .shareInBackground()

    override val buttonState: Flow<DescriptiveButtonState> = flowOf {
        DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
    }
    override fun continueClicked() {
        accountRouter.openFinishImportParitySigner(payload)
    }
}
