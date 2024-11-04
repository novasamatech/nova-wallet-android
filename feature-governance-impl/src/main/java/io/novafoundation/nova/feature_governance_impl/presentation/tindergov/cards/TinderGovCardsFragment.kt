package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.viewBinding
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.StackFrom
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.common.view.shape.toColorStateList
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.FragmentTinderGovCardsBinding
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureComponent
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardsAdapter

class TinderGovCardsFragment : BaseFragment<TinderGovCardsViewModel, FragmentTinderGovCardsBinding>(), TinderGovCardsAdapter.Handler, TinderGovCardStackListener {

    override val binder by viewBinding(FragmentTinderGovCardsBinding::bind)

    private val adapter = TinderGovCardsAdapter(lifecycleOwner = this, handler = this)

    override fun initViews() {
        binder.tinderGovCardsStatusBarInsetsContainer.applyStatusBarInsets()
        binder.tinderGovCardsBack.setOnClickListener { viewModel.back() }
        binder.tinderGovCardsSettings.setOnClickListener { viewModel.editVotingPowerClicked() }

        binder.tinderGovCardsStack.adapter = adapter
        binder.tinderGovCardsStack.itemAnimator = null

        binder.tinderGovCardsStack.layoutManager = CardStackLayoutManager(requireContext(), this)
            .apply {
                setStackFrom(StackFrom.Bottom)
                setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
                setVisibleCount(TinderGovCardsViewModel.CARD_STACK_SIZE)
                setTranslationInterval(8f)
                setScaleInterval(getScaleIntervalForPadding(16.dp))
                setOverlayInterpolator(CardsOverlayInterpolator(delay = 0.15f, maxResult = 0.64f))
            }

        binder.tinderGovCardsControlView.setAyeClickListener { swipeCardToDirection(Direction.Right) }
        binder.tinderGovCardsControlView.setAbstainClickListener { swipeCardToDirection(Direction.Top) }
        binder.tinderGovCardsControlView.setNayClickListener { swipeCardToDirection(Direction.Left) }

        binder.tinderGovCardsBasketButton.setOnClickListener { viewModel.onBasketClicked() }
        binder.tinderGovCardsEmptyStateButton.setOnClickListener { viewModel.onBasketClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GovernanceFeatureComponent>(
            requireContext(),
            GovernanceFeatureApi::class.java
        )
            .tinderGovCardsFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: TinderGovCardsViewModel) {
        viewModel.referendumCounterFlow.observe {
            binder.tinderGovCardsSubtitle.text = it
        }

        viewModel.cardsFlow.observe { adapter.submitList(it) }

        viewModel.placeholderTextFlow.observe {
            binder.tinderGovCardsEmptyStateDescription.text = it
        }

        viewModel.showConfirmButtonFlow.observe {
            binder.tinderGovCardsEmptyStateButton.isVisible = it
        }

        viewModel.skipCardEvent.observeEvent {
            swipeCardToDirection(Direction.Bottom, forced = true)
        }

        viewModel.rewindCardEvent.observeEvent {
            binder.tinderGovCardsStack.rewind()
        }

        viewModel.resetCardsEvent.observeEvent {
            binder.tinderGovCardsStack.cardLayoutManager().topPosition = 0
        }

        viewModel.isCardDraggingAvailable.observe { draggingAvailable ->
            binder.tinderGovCardsStack.cardLayoutManager()
                .apply {
                    setCanScrollHorizontal(draggingAvailable)
                    setCanScrollVertical(draggingAvailable)
                }
        }

        viewModel.basketModelFlow.observe {
            binder.tinderGovCardsBasketItems.text = it.items.toString()
            binder.tinderGovCardsBasketItems.setTextColorRes(it.textColorRes)
            binder.tinderGovCardsBasketItems.backgroundTintList = requireContext().getColor(it.backgroundColorRes).toColorStateList()

            binder.tinderGovCardsBasketState.setText(it.textRes)
            binder.tinderGovCardsBasketState.setTextColorRes(it.textColorRes)

            binder.tinderGovCardsBasketChevron.setImageTintRes(it.imageTintRes)
        }

        viewModel.insufficientBalanceChangeAction.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                positiveTextRes = R.string.common_change,
                negativeTextRes = R.string.common_close,
                styleRes = R.style.AccentAlertDialogTheme
            ) {
                setTitle(it.payload.first)
                setMessage(it.payload.second)
            }
        }

        viewModel.retryReferendumInfoLoadingAction.awaitableActionLiveData.observeEvent {
            warningDialog(
                requireContext(),
                onPositiveClick = { it.onSuccess(true) },
                onNegativeClick = { it.onSuccess(false) },
                positiveTextRes = R.string.common_retry,
                negativeTextRes = R.string.common_skip,
                styleRes = R.style.AccentAlertDialogTheme
            ) {
                setTitle(R.string.swipe_gov_card_data_loading_error_title)
                setMessage(R.string.swipe_gov_card_data_loading_error_message)
            }
        }

        viewModel.hasReferendaToVote.observe {
            binder.tinderGovCardsSettings.isVisible = it
            binder.tinderGovCardsControlView.setVisible(it, falseState = View.INVISIBLE)

            // To avoid click if referenda cards is empty
            binder.tinderGovCardsStack.isEnabled = it
        }
    }

    private fun getScaleIntervalForPadding(desiredPadding: Int): Float {
        val screenWidth = resources.displayMetrics.widthPixels
        val cardsPadding = binder.tinderGovCardsStack.paddingStart + binder.tinderGovCardsStack.paddingEnd
        val cardWidth = screenWidth - cardsPadding
        val nextCardWidth = cardWidth - desiredPadding * 2
        return nextCardWidth.toFloat() / cardWidth.toFloat()
    }

    override fun onReadMoreClicked(item: TinderGovCardRvItem) {
        viewModel.openReadMore(item)
    }

    override fun onCardAppeared(view: View?, position: Int) {
        viewModel.onCardAppeared(position)
    }

    override fun onCardSwiped(direction: Direction) {
        val topPosition = binder.tinderGovCardsStack.cardLayoutManager().topPosition
        val swipedPosition = topPosition - 1
        when (direction) {
            Direction.Left -> viewModel.nayClicked(swipedPosition)
            Direction.Right -> viewModel.ayeClicked(swipedPosition)
            Direction.Top -> viewModel.abstainClicked(swipedPosition)
            Direction.Bottom -> {}
        }
    }

    private fun swipeCardToDirection(direction: Direction, forced: Boolean = false) {
        val layoutManager = binder.tinderGovCardsStack.cardLayoutManager()
        if (forced || layoutManager.canScrollVertically() && layoutManager.canScrollHorizontally()) {
            val setting = SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            layoutManager.setSwipeAnimationSetting(setting)
            binder.tinderGovCardsStack.swipe()
        }
    }

    private fun CardStackView.cardLayoutManager(): CardStackLayoutManager {
        return layoutManager as CardStackLayoutManager
    }
}

class CardsOverlayInterpolator(private val delay: Float, private val maxResult: Float) : DecelerateInterpolator() {

    override fun getInterpolation(input: Float): Float {
        val realInput = (input - delay).coerceAtLeast(0f)
        val result = super.getInterpolation(realInput)

        return (result * maxResult).coerceAtMost(maxResult)
    }
}
