package io.novafoundation.nova.feature_staking_impl.di

import dagger.Module
import io.novafoundation.nova.feature_staking_impl.di.validations.AddStakingProxyValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.BondMoreValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.MakePayoutValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.RebondValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.RedeemValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.RemoveStakingProxyValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.RewardDestinationValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.SetControllerValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.SetupStakingValidationsModule
import io.novafoundation.nova.feature_staking_impl.di.validations.StakeActionsValidationModule
import io.novafoundation.nova.feature_staking_impl.di.validations.UnbondValidationsModule

@Module(
    includes = [
        MakePayoutValidationsModule::class,
        SetupStakingValidationsModule::class,
        BondMoreValidationsModule::class,
        UnbondValidationsModule::class,
        RedeemValidationsModule::class,
        RebondValidationsModule::class,
        SetControllerValidationsModule::class,
        RewardDestinationValidationsModule::class,
        StakeActionsValidationModule::class,
        AddStakingProxyValidationsModule::class,
        RemoveStakingProxyValidationsModule::class
    ]
)
class StakingValidationModule
