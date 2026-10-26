package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.FragmentBittensorWhereToStakeBinding
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureComponent
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.observeBittensorRetries

class BittensorWhereToStakeFragment : BaseFragment<BittensorWhereToStakeViewModel, FragmentBittensorWhereToStakeBinding>() {

    private val cardsAdapter = BittensorWhereToStakeAdapter(::choose)

    private var selectedPage = 0

    override fun createBinding() = FragmentBittensorWhereToStakeBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<StakingFeatureComponent>(requireContext(), StakingFeatureApi::class.java)
            .bittensorSubnetComponentFactory()
            .create(this, BittensorChooseSubnetPayload())
            .inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedPage = savedInstanceState?.getInt(SELECTED_PAGE) ?: 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_PAGE, selectedPage)
        super.onSaveInstanceState(outState)
    }

    override fun initViews() = with(binder) {
        toolbar.setHomeButtonListener { viewModel.back() }
        onBackPressed { viewModel.back() }
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val snapHelper = PagerSnapHelper()
        cards.layoutManager = layoutManager
        cards.adapter = cardsAdapter
        cards.itemAnimator = null
        cards.updateLayoutParams {
            height = (548 * resources.configuration.fontScale.coerceAtLeast(1f)).toInt().dp(requireContext())
        }
        snapHelper.attachToRecyclerView(cards)
        cards.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                val snappedView = snapHelper.findSnapView(layoutManager) ?: return
                selectedPage = layoutManager.getPosition(snappedView)
                showPage()
            }
        })
        cards.scrollToPosition(selectedPage)
        previous.setOnClickListener { movePage(-1) }
        next.setOnClickListener { movePage(1) }
        showPage()
    }

    override fun subscribe(viewModel: BittensorWhereToStakeViewModel) = with(binder) {
        observeBittensorRetries(viewModel.retryEvents)
        viewModel.nativeSymbol.observe { cardsAdapter.nativeSymbol = it }
    }

    override fun onDestroyView() {
        binder.cards.adapter = null
        super.onDestroyView()
    }

    private fun choose(option: BittensorWhereToStakeOption) {
        when (option) {
            BittensorWhereToStakeOption.POOL_SIZE -> viewModel.quickPick(BittensorQuickPickOrder.POOL_SIZE)
            BittensorWhereToStakeOption.AGE -> viewModel.quickPick(BittensorQuickPickOrder.AGE)
            BittensorWhereToStakeOption.ROOT -> viewModel.rootStaking()
            BittensorWhereToStakeOption.APY -> viewModel.quickPick(BittensorQuickPickOrder.APY)
        }
    }

    private fun movePage(offset: Int) {
        selectedPage = (selectedPage + offset).coerceIn(0, cardsAdapter.itemCount - 1)
        binder.cards.smoothScrollToPosition(selectedPage)
        showPage()
    }

    private fun showPage() = with(binder) {
        cardsAdapter.selectedPage = selectedPage
        previous.isEnabled = selectedPage > 0
        previous.alpha = if (previous.isEnabled) 1f else 0.4f
        next.isEnabled = selectedPage < cardsAdapter.itemCount - 1
        next.alpha = if (next.isEnabled) 1f else 0.4f
        indicators.removeAllViews()
        repeat(cardsAdapter.itemCount) { index ->
            val selected = index == selectedPage
            val dot = View(requireContext())
            dot.background = GradientDrawable().apply {
                cornerRadius = 3.dp(requireContext()).toFloat()
                setColor(requireContext().getColor(if (selected) R.color.icon_accent else R.color.icon_inactive))
            }
            val layout = LinearLayout.LayoutParams((if (selected) 20 else 6).dp(requireContext()), 6.dp(requireContext()))
            layout.marginEnd = if (index < cardsAdapter.itemCount - 1) 6.dp(requireContext()) else 0
            indicators.addView(dot, layout)
        }
    }

    private companion object {

        const val SELECTED_PAGE = "BittensorWhereToStakeFragment.SelectedPage"
    }
}
