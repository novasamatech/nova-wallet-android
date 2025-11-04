package io.novafoundation.nova.feature_gift_impl.presentation.claim

import android.animation.Animator
import android.view.View
import androidx.core.view.postDelayed
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentClaimGiftBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload
import javax.inject.Inject

private const val HIDE_ANIMATION_DURATION = 400L

class ClaimGiftFragment : BaseFragment<ClaimGiftViewModel, FragmentClaimGiftBinding>() {

    companion object : PayloadCreator<ShareGiftPayload> by FragmentPayloadCreator()

    private val giftAnimationListener = object : Animator.AnimatorListener {
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {
            viewModel.onGiftClaimedAnimationFinished()
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun createBinding() = FragmentClaimGiftBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.claimGiftToolbar.setHomeButtonListener { viewModel.back() }

        binder.claimGiftButton.setOnClickListener { viewModel.claimGift() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .claimGiftComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: ClaimGiftViewModel) {
        viewModel.giftAnimationRes.observe {
            binder.claimGiftAnimation.setAnimation(it)
            binder.claimGiftAnimation.playAnimation()
        }

        viewModel.amountModel.observe {
            binder.claimGiftTokenIcon.setTokenIcon(it.tokenIcon, imageLoader)
            binder.claimGiftAmount.text = it.amount
        }

        viewModel.giftClaimedEvent.observeEvent {
            hideAllViewsWithAnimation()
        }
    }

    private fun hideAllViewsWithAnimation() {
        binder.claimGiftToolbar.hideWithAnimation()
        binder.claimGiftTokenIcon.hideWithAnimation()
        binder.claimGiftAmount.hideWithAnimation()
        binder.claimGiftButton.hideWithAnimation()
        binder.claimGiftTitle.hideWithAnimation()
        binder.claimGiftAccount.hideWithAnimation()

        binder.root.postDelayed(HIDE_ANIMATION_DURATION) {

        }
    }

    private fun View.hideWithAnimation() {
        animate().alpha(0f)
            .setDuration(HIDE_ANIMATION_DURATION)
            .withEndAction { makeGone() }
            .start()
    }
}
