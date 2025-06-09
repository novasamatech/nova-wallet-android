package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.MultisigFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.hasAccountComparator
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.withChainComparator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MultisigWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val multisigFormatter: MultisigFormatter,
    metaAccount: MultisigMetaAccount
) : WalletDetailsMixin(metaAccount) {
    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        val signatory = interactor.getMetaAccount(metaAccount.signatoryMetaId)
        val signatoryWithIcon = multisigFormatter.formatSignatorySubtitle(signatory)

        // TODO multisig: we might want to show other signatories and threshold here
        AlertModel(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_multisig
            ),
            message = resourceManager.getString(R.string.multisig_wallet_details_info_warning),
            subMessage = SpannableStringBuilder(signatoryWithIcon)
        )
    }

    override fun accountProjectionsFlow(): Flow<List<Any?>> = interactor.allPresentChainProjections(metaAccount).map { accounts ->
        val availableActions = availableAccountActions.first()

        accounts.map {
            accountFormatter.formatChainAccountProjection(it, availableActions)
        }
    }
}
