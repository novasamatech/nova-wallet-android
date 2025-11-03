package io.novafoundation.nova.feature_gift_impl.presentation.share

import android.animation.Animator
import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.utils.share.shareImageWithText
import io.novafoundation.nova.feature_account_api.presenatation.chain.setTokenIcon
import io.novafoundation.nova.feature_gift_api.di.GiftFeatureApi
import io.novafoundation.nova.feature_gift_impl.databinding.FragmentShareGiftBinding
import io.novafoundation.nova.feature_gift_impl.di.GiftFeatureComponent
import javax.inject.Inject

class ShareGiftFragment : BaseFragment<ShareGiftViewModel, FragmentShareGiftBinding>() {

    companion object : PayloadCreator<ShareGiftPayload> by FragmentPayloadCreator()

    @Inject
    lateinit var imageLoader: ImageLoader

    private val giftAnimationListener = object : Animator.AnimatorListener {
        override fun onAnimationCancel(animation: Animator) {}
        override fun onAnimationRepeat(animation: Animator) {}
        override fun onAnimationStart(animation: Animator) {}

        override fun onAnimationEnd(animation: Animator) {
            showAllViewsWithAnimation()
        }
    }

    override fun createBinding() = FragmentShareGiftBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.shareGiftToolbar.setHomeButtonListener { viewModel.back() }

        binder.shareGiftButton.setOnClickListener { viewModel.shareDeepLinkClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<GiftFeatureComponent>(this, GiftFeatureApi::class.java)
            .shareGiftComponentFactory()
            .create(this, payload())
            .inject(this)
    }

    override fun subscribe(viewModel: ShareGiftViewModel) {
        viewModel.giftAnimationRes.observe {
            binder.shareGiftAnimation.setAnimation(it)
            binder.shareGiftAnimation.playAnimation()
            binder.shareGiftAnimation.addAnimatorListener(giftAnimationListener)
        }

        viewModel.amountModel.observe {
            binder.shareGiftTokenIcon.setTokenIcon(it.tokenIcon, imageLoader)
            binder.shareGiftAmount.text = it.amount
        }

        viewModel.shareEvent.observeEvent {
            shareImageWithText(sharingData = it, chooserTitle = null)
        }
    }

    private fun showAllViewsWithAnimation() {
        binder.shareGiftToolbar.showWithAnimation()
        binder.shareGiftTokenIcon.showWithAnimation()
        binder.shareGiftAmount.showWithAnimation()
        binder.shareGiftButton.showWithAnimation()
        binder.shareGiftTitle.showWithAnimation()
    }

    private fun View.showWithAnimation() {
        animate().alpha(1f)
            .setDuration(400)
            .start()
    }
}
