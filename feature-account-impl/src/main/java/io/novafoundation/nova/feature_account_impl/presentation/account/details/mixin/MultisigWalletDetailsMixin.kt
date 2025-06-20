package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.append
import io.novafoundation.nova.common.utils.appendEnd
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.common.view.AlertView
import io.novafoundation.nova.feature_account_api.domain.model.MultisigAvailability
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.allSignatories
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.delegated.MultisigFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MultisigWalletDetailsMixin(
    private val resourceManager: ResourceManager,
    private val accountFormatterFactory: AccountFormatterFactory,
    private val interactor: WalletDetailsInteractor,
    private val multisigFormatter: MultisigFormatter,
    private val host: WalletDetailsMixinHost,
    private val chainRegistry: ChainRegistry,
    private val coroutineScope: CoroutineScope,
    metaAccount: MultisigMetaAccount
) : WalletDetailsMixin(metaAccount), CoroutineScope by coroutineScope {

    private val accountFormatter = accountFormatterFactory.create(baseAccountTitleFormatter(resourceManager))

    override val availableAccountActions: Flow<Set<AccountAction>> = flowOf { emptySet() }

    override val typeAlert: Flow<AlertModel?> = flowOf {
        AlertModel(
            style = AlertView.Style(
                backgroundColorRes = R.color.block_background,
                iconRes = R.drawable.ic_multisig
            ),
            message = mainMessage(metaAccount),
            subMessages = subMessage(metaAccount)
        )
    }

    override fun accountProjectionsFlow(): Flow<List<Any?>> = interactor.allPresentChainProjections(metaAccount).map { accounts ->
        val availableActions = availableAccountActions.first()

        accounts.map {
            accountFormatter.formatChainAccountProjection(it, availableActions)
        }
    }

    private fun mainMessage(metaAccount: MultisigMetaAccount): String {
        val threshold = metaAccount.threshold
        val allSignatoriesQuantity = metaAccount.allSignatories().size
        return resourceManager.getString(R.string.multisig_wallet_details_info_warning_title, threshold, allSignatoriesQuantity)
    }

    private suspend fun subMessage(metaAccount: MultisigMetaAccount): List<CharSequence> {
        val message = resourceManager.getString(R.string.multisig_wallet_details_info_warning)
        val userSignatory = getMetaAccountFormat(metaAccount.signatoryMetaId)
        val otherSignatoriesTitle = resourceManager.getString(R.string.multisig_wallet_details_info_warning_other_signatories)
        val otherSignatories = metaAccount.otherSignatories.map { getAccountFormat(metaAccount, it) }

        return buildList {
            add(message.withPrimaryColor())
            add(userSignatory)
            add(otherSignatoriesTitle)
            addAll(otherSignatories)
        }
    }

    private fun CharSequence.withPrimaryColor() = toSpannable(colorSpan(resourceManager.getColor(R.color.text_primary)))

    private suspend fun getMetaAccountFormat(metaId: Long): CharSequence {
        val signatory = interactor.getMetaAccount(metaId)
        return multisigFormatter.formatSignatory(signatory)
            .toSpannable(colorSpan(resourceManager.getColor(R.color.text_primary)))
    }

    private suspend fun getAccountFormat(metaAccount: MultisigMetaAccount, accountIdKey: AccountIdKey): CharSequence {
        val chain = getChain(metaAccount)
        val accountDrawable = multisigFormatter.makeAccountDrawable(accountIdKey.value)
        val accountAddress = chain.addressOf(accountIdKey.value)
            .ellipsizeMiddle(shownSymbols = 9)

        val addressColor = resourceManager.getColor(R.color.text_secondary)
        val infoIcon = resourceManager.getDrawable(R.drawable.ic_info).apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }

        return SpannableStringBuilder()
            .appendEnd(drawableSpan(accountDrawable))
            .appendSpace()
            .append(accountAddress, colorSpan(addressColor))
            .appendSpace()
            .appendEnd(drawableSpan(infoIcon))
            .setFullSpan(
                clickableSpan {
                    showAddress(accountIdKey, chain)
                }
            )
    }

    private fun showAddress(accountIdKey: AccountIdKey, chain: Chain) {
        launch {
            host.externalActions.showAddressActions(accountIdKey.value, chain)
        }
    }

    private fun CharSequence.ellipsizeMiddle(shownSymbols: Int): CharSequence {
        if (length < shownSymbols * 2) return this

        return SpannableStringBuilder(this.subSequence(0, shownSymbols))
            .append("...")
            .append(this.substring(length - shownSymbols))
    }

    private suspend fun getChain(metaAccount: MultisigMetaAccount): Chain {
        return when (val availability = metaAccount.availability) {
            MultisigAvailability.Universal -> chainRegistry.getChain(ChainGeneses.POLKADOT)
            is MultisigAvailability.SingleChain -> chainRegistry.getChain(availability.chainId)
        }
    }
}
