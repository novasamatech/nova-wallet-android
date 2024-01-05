package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_ADD_PROXY
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_CONTROLLER
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PAYOUTS
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PROXIES
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_REWARD_DESTINATION
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_VALIDATORS

class ManageStakeAction(
    val id: String,
    val label: String,
    @DrawableRes val iconRes: Int,
    val badge: String? = null,
) {
    companion object
}

fun ManageStakeAction.Companion.bondMore(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_STAKING_BOND_MORE,
        label = resourceManager.getString(R.string.staking_bond_more_v1_9_0),
        iconRes = R.drawable.ic_add_circle_outline
    )
}

fun ManageStakeAction.Companion.unbond(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_STAKING_UNBOND,
        label = resourceManager.getString(R.string.staking_unbond_v1_9_0),
        iconRes = R.drawable.ic_minus_circle_outline
    )
}

fun ManageStakeAction.Companion.rewardDestination(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_REWARD_DESTINATION,
        label = resourceManager.getString(R.string.staking_rewards_destination_title_v2_0_0),
        iconRes = R.drawable.ic_buy_outline
    )
}

fun ManageStakeAction.Companion.payouts(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_PAYOUTS,
        label = resourceManager.getString(R.string.staking_reward_payouts_title_v2_2_0),
        iconRes = R.drawable.ic_unpaid_rewards
    )
}

fun ManageStakeAction.Companion.validators(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_VALIDATORS,
        label = resourceManager.getString(R.string.staking_your_validators),
        iconRes = R.drawable.ic_validators_outline
    )
}

fun ManageStakeAction.Companion.controller(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_CONTROLLER,
        label = resourceManager.getString(R.string.staking_controller_account),
        iconRes = R.drawable.ic_people_outline
    )
}

fun ManageStakeAction.Companion.addStakingProxy(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_ADD_PROXY,
        label = resourceManager.getString(R.string.staking_action_add_proxy),
        iconRes = R.drawable.ic_delegate_outline
    )
}

fun ManageStakeAction.Companion.stakingProxies(resourceManager: ResourceManager): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_PROXIES,
        label = resourceManager.getString(R.string.staking_action_your_proxies),
        iconRes = R.drawable.ic_people_outline
    )
}
