package io.novafoundation.nova.feature_assets.presentation.transaction.detail.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.extrinsic.ExtrinsicDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.direct.RewardDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.reward.pool.PoolRewardDetailFragment
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.transfer.TransferDetailFragment

@Subcomponent(
    modules = [
        TransactionDetailModule::class,
    ]
)
@ScreenScope
interface TransactionDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance operation: OperationParcelizeModel.Transfer
        ): TransactionDetailComponent
    }

    fun inject(fragment: TransferDetailFragment)
}

@Subcomponent(
    modules = [
        RewardDetailModule::class
    ]
)
@ScreenScope
interface RewardDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance operation: OperationParcelizeModel.Reward
        ): RewardDetailComponent
    }

    fun inject(fragment: RewardDetailFragment)
}

@Subcomponent(
    modules = [
        PoolRewardDetailModule::class
    ]
)
@ScreenScope
interface PoolRewardDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance operation: OperationParcelizeModel.PoolReward
        ): PoolRewardDetailComponent
    }

    fun inject(fragment: PoolRewardDetailFragment)
}

@Subcomponent(
    modules = [
        ExtrinsicDetailModule::class
    ]
)
@ScreenScope
interface ExtrinsicDetailComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance extrinsic: OperationParcelizeModel.Extrinsic
        ): ExtrinsicDetailComponent
    }

    fun inject(fragment: ExtrinsicDetailFragment)
}
