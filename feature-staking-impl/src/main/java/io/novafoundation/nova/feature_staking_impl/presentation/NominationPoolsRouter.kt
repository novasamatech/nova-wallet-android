package io.novafoundation.nova.feature_staking_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.confirm.NominationPoolsConfirmBondMorePayload

interface NominationPoolsRouter : ReturnableRouter {

    fun openSetupBondMore()

    fun openConfirmBondMore(payload: NominationPoolsConfirmBondMorePayload)

    fun openSetupUnbond()

    fun returnToStakingMain()
}
