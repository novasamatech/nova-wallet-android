package io.novafoundation.nova.feature_account_impl.presentation.account.details.mixin

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.headers.TextHeader
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.view.AlertModel
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.chain.model.AccountInChainUi
import io.novafoundation.nova.feature_account_api.presenatation.account.details.ChainAccountActionsSheet.AccountAction
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountInChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletDetailsMixinHost(
    val browserableDelegate: Browserable.Presentation
)

abstract class WalletDetailsMixin(
    val metaAccount: MetaAccount
) {

    abstract val availableAccountActions: Flow<Set<AccountAction>>

    abstract val typeAlert: Flow<AlertModel?>

    val chainAccountProjections: Flow<List<Any>> = accountProjectionsFlow()
        .map { groupedList ->
            groupedList.toListWithHeaders(
                keyMapper = { type, _ -> mapAccountHeader(type) },
                valueMapper = { mapAccount(it) }
            )
        }

    abstract fun accountProjectionsFlow(): Flow<GroupedList<AccountInChain.From, AccountInChain>>

    abstract suspend fun mapAccountHeader(from: AccountInChain.From): TextHeader?

    abstract suspend fun mapAccount(accountInChain: AccountInChain): AccountInChainUi
}
