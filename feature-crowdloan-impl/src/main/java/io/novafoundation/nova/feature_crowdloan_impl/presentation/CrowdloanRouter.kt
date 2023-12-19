package io.novafoundation.nova.feature_crowdloan_impl.presentation

import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.parcel.ConfirmContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.BonusPayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.model.CustomContributePayload
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.parcel.ContributePayload
import kotlinx.coroutines.flow.Flow

interface CrowdloanRouter {

    fun openContribute(payload: ContributePayload)

    val customBonusFlow: Flow<BonusPayload?>

    val latestCustomBonus: BonusPayload?

    fun openCustomContribute(payload: CustomContributePayload)

    fun setCustomBonus(payload: BonusPayload)

    fun openConfirmContribute(payload: ConfirmContributePayload)

    fun back()

    fun returnToMain()

    fun openMoonbeamFlow(payload: ContributePayload)
    fun openAddAccount(payload: AddAccountPayload)

    fun openUserContributions()

    fun openSwitchWallet()

    fun openWalletDetails(metaId: Long)
}
