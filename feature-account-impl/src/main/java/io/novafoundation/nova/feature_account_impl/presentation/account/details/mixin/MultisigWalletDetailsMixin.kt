package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import android.text.SpannableStringBuilder
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.asAccountId
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.UNIFIED_ADDRESS_PREFIX
import io.novafoundation.nova.common.utils.append
import io.novafoundation.nova.common.utils.appendEnd
import io.novafoundation.nova.common.utils.appendSpace
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.drawableSpan
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.launchUnit
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
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.delegeted.MultisigFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.AccountFormatterFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin.common.baseAccountTitleFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.util.forChain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
        val addressFormat = getAddressFormat(metaAccount)
        val accountDrawable = multisigFormatter.makeAccountDrawable(accountIdKey.value)
        val accountAddress = addressFormat.addressOf(accountIdKey.value.asAccountId())
            .value
            .ellipsizeAddress()

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
                    showAddress(metaAccount, accountIdKey, addressFormat)
                }
            )
    }

    private fun showAddress(metaAccount: MultisigMetaAccount, accountIdKey: AccountIdKey, addressFormat: AddressFormat) = launchUnit {
        when (val availability = metaAccount.availability) {
            is MultisigAvailability.Universal -> host.addressActionsMixin.showAddressActions(accountIdKey.value, addressFormat)
            is MultisigAvailability.SingleChain -> {
                val chain = chainRegistry.getChain(availability.chainId)
                host.externalActions.showAddressActions(accountIdKey.value, chain)
            }
        }
    }

    private suspend fun getAddressFormat(metaAccount: MultisigMetaAccount): AddressFormat {
        return when (val availability = metaAccount.availability) {
            is MultisigAvailability.Universal -> AddressFormat.defaultForScheme(
                availability.addressScheme,
                substrateAddressPrefix = SS58Encoder.UNIFIED_ADDRESS_PREFIX
            )

            is MultisigAvailability.SingleChain -> {
                val chain = chainRegistry.getChain(availability.chainId)
                AddressFormat.forChain(chain)
            }
        }
    }
}
