package io.novafoundation.nova.feature_gift_impl.presentation.claim

import android.animation.Animator
import android.view.View
import androidx.core.view.postDelayed
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.view.setModelOrHide
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectSingleWallet.bindSelectWallet
import io.novafoundation.nova.feature_account_api.view.setSelectable
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentClaimGiftBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import javax.inject.Inject

private const val HIDE_ANIMATION_DURATION = 400L
private const val UNPACKING_START_FRAME = 180

class ClaimGiftFragment : BaseFragment<ClaimGiftViewModel, FragmentClaimGiftBinding>() {

    companion object : PayloadCreator<ClaimGiftPayload> by FragmentPayloadCreator()

    private val giftAnimationListener = object : Animator.AnimatorListener {
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {
            viewModel.onGiftClaimAnimationFinished()
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun createBinding() = FragmentClaimGiftBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.claimGiftToolbar.setHomeButtonListener { viewModel.back() }

        binder.claimGiftButton.setOnClickListener { viewModel.claimGift() }
        binder.claimGiftButton.prepareForProgress(this)
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .claimGiftComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: ClaimGiftViewModel) {
        bindSelectWallet(viewModel.selectWalletMixin) { isAvailableToSelect ->
            binder.claimGiftAccount.setSelectable(isAvailableToSelect) {
                viewModel.selectWalletToClaim()
            }
            binder.claimGiftAccount.setActionTint(R.color.icon_secondary)
        }

        viewModel.giftAnimationRes.observe {
            binder.claimGiftAnimation.setMinAndMaxFrame(0, UNPACKING_START_FRAME)
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

        viewModel.selectedWalletModel.observe {
            binder.claimGiftAccount.setModel(it)
        }

        viewModel.confirmButtonStateFlow.observe {
            binder.claimGiftButton.setState(it)
        }

        viewModel.alertModelFlow.observe {
            binder.claimGiftAlert.setModelOrHide(it)
        }
    }

    private fun hideAllViewsWithAnimation() {
        binder.claimGiftToolbar.hideWithAnimation()
        binder.claimGiftTokenIcon.hideWithAnimation()
        binder.claimGiftAmount.hideWithAnimation()
        binder.claimGiftButton.hideWithAnimation()
        binder.claimGiftTitle.hideWithAnimation()
        binder.claimGiftAccountTitle.hideWithAnimation()
        binder.claimGiftAccount.hideWithAnimation()

        binder.root.postDelayed(HIDE_ANIMATION_DURATION) {
            val maxFrame = binder.claimGiftAnimation.composition?.endFrame?.toInt() ?: 0
            binder.claimGiftAnimation.setMinAndMaxFrame(UNPACKING_START_FRAME, maxFrame)
            binder.claimGiftAnimation.addAnimatorListener(giftAnimationListener)
            binder.claimGiftAnimation.playAnimation()
        }
    }

    private fun View.hideWithAnimation() {
        animate().alpha(0f)
            .setDuration(HIDE_ANIMATION_DURATION)
            .withEndAction { makeInvisible() }
            .start()
    }
}
