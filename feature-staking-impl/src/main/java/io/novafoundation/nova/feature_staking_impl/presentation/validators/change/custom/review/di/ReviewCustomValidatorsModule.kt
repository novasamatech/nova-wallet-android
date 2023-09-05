package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.common.CustomValidatorsPayload
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ChangeStakingReviewValidatorsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction.EmptyReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewCustomValidatorsViewModel
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction.ReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.ReviewValidatorsRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction.SetupStakingReviewValidatorsFlowAction
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.SetupStakingReviewValidatorsRouter
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase

@Module(includes = [ViewModelModule::class])
class ReviewCustomValidatorsModule {

    @Provides
    fun provideReviewValidatorsRouter(
        router: StakingRouter,
        payload: CustomValidatorsPayload
    ): ReviewValidatorsRouter {
        return when (payload.flowType) {
            CustomValidatorsPayload.FlowType.SETUP_STAKING_VALIDATORS -> SetupStakingReviewValidatorsRouter(router)
            CustomValidatorsPayload.FlowType.CHANGE_STAKING_VALIDATORS -> ChangeStakingReviewValidatorsRouter(router)
        }
    }

    @Provides
    fun provideReviewValidatorsFlowAction(
        payload: CustomValidatorsPayload,
        setupStakingSharedState: SetupStakingSharedState,
        setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory
    ): ReviewValidatorsFlowAction {
        return when (payload.flowType) {
            CustomValidatorsPayload.FlowType.SETUP_STAKING_VALIDATORS -> SetupStakingReviewValidatorsFlowAction(
                setupStakingSharedState,
                setupStakingTypeSelectionMixinFactory
            )

            CustomValidatorsPayload.FlowType.CHANGE_STAKING_VALIDATORS -> EmptyReviewValidatorsFlowAction()
        }
    }

    @Provides
    @IntoMap
    @ViewModelKey(ReviewCustomValidatorsViewModel::class)
    fun provideViewModel(
        addressIconGenerator: AddressIconGenerator,
        stakingInteractor: StakingInteractor,
        resourceManager: ResourceManager,
        router: StakingRouter,
        reviewValidatorsRouter: ReviewValidatorsRouter,
        setupStakingSharedState: SetupStakingSharedState,
        tokenUseCase: TokenUseCase,
        selectedAssetState: StakingSharedState,
        reviewValidatorsFlowAction: ReviewValidatorsFlowAction
    ): ViewModel {
        return ReviewCustomValidatorsViewModel(
            router,
            reviewValidatorsRouter,
            addressIconGenerator,
            stakingInteractor,
            resourceManager,
            setupStakingSharedState,
            selectedAssetState,
            reviewValidatorsFlowAction,
            tokenUseCase
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): ReviewCustomValidatorsViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReviewCustomValidatorsViewModel::class.java)
    }
}
