package io.novafoundation.nova.feature_account_impl.presentation.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.presentation.masking.formatter.MaskableValueFormatterProvider
import io.novafoundation.nova.common.presentation.masking.getUnmaskedOrElse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class RealSelectedAccountUseCase(
    private val accountRepository: AccountRepository,
    private val walletUiUseCase: WalletUiUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
    private val accountTypePresentationMapper: MetaAccountTypePresentationMapper,
    private val maskableValueFormatterProvider: MaskableValueFormatterProvider,
    private val resourceManager: ResourceManager
) : SelectedAccountUseCase {

    override fun selectedMetaAccountFlow(): Flow<MetaAccount> = accountRepository.selectedMetaAccountFlow()

    override fun selectedAddressModelFlow(chain: suspend () -> Chain): Flow<AddressModel> = selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = chain(),
            account = it,
            name = null
        )
    }

    override fun selectedWalletModelFlow(): Flow<SelectedWalletModel> = combine(
        selectedMetaAccountFlow(),
        metaAccountsUpdatesRegistry.observeUpdatesExist(),
        maskableValueFormatterProvider.provideFormatter()
    ) { metaAccount, hasMetaAccountsUpdates, maskableValueFormatter ->
        val icon = walletUiUseCase.walletIcon(metaAccount, transparentBackground = false)
        val typeIcon = accountTypePresentationMapper.iconFor(metaAccount.type)

        SelectedWalletModel(
            typeIcon = typeIcon,
            walletIcon = maskableValueFormatter.format { icon }
                .getUnmaskedOrElse { resourceManager.getDrawable(R.drawable.ic_identicon_placeholder_with_background) },
            name = metaAccount.name,
            hasUpdates = hasMetaAccountsUpdates
        )
    }

    override suspend fun getSelectedMetaAccount(): MetaAccount = accountRepository.getSelectedMetaAccount()
}
