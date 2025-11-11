package io.novafoundation.nova.feature_gift_impl.presentation.share

import android.animation.Animator
import android.view.View
import coil.ImageLoader
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.common.utils.share.shareImageWithText
import io.novafoundation.nova.common.view.dialog.warningDialog
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
            showAllViews()
        }
    }

    override fun createBinding() = FragmentShareGiftBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.shareGiftToolbar.setHomeButtonListener { viewModel.back() }
        binder.shareGiftToolbar.setRightActionClickListener { viewModel.reclaimClicked() }

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
            binder.shareGiftAnimation.setAnimation(it.res)
            when (it.state) {
                ShareGiftAnimationState.State.START -> {
                    binder.shareGiftAnimation.playAnimation()
                    binder.shareGiftAnimation.addAnimatorListener(giftAnimationListener)
                }

                ShareGiftAnimationState.State.IDLE_END -> {
                    binder.shareGiftAnimation.progress = 1f
                    showAllViews(withAnimation = false)
                }
            }
        }

        viewModel.amountModel.observe {
            binder.shareGiftTokenIcon.setTokenIcon(it.tokenIcon, imageLoader)
            binder.shareGiftAmount.text = it.amount
        }

        viewModel.shareEvent.observeEvent {
            shareImageWithText(sharingData = it, chooserTitle = null)
        }

        viewModel.isReclaimInProgress.observe {
            binder.shareGiftToolbar.showProgress(it)
        }

        viewModel.reclaimButtonVisible.observe {
            binder.shareGiftToolbar.setRightTextVisible(it)
        }

        viewModel.confirmReclaimGiftAction.awaitableActionLiveData.observeEvent { event ->
            warningDialog(
                context = providedContext,
                onPositiveClick = { event.onSuccess(Unit) },
                positiveTextRes = R.string.common_continue,
                negativeTextRes = R.string.common_cancel,
                onNegativeClick = { event.onCancel() },
                styleRes = R.style.AccentAlertDialogTheme
            ) {
                setTitle(getString(R.string.reclaim_gift_confirmation_title, event.payload.amount))

                setMessage(R.string.reclaim_gift_confirmation_message)
            }
        }
    }

    private fun showAllViews(withAnimation: Boolean = true) {
        binder.shareGiftToolbar.show(withAnimation)
        binder.shareGiftTokenIcon.show(withAnimation)
        binder.shareGiftAmount.show(withAnimation)
        binder.shareGiftButton.show(withAnimation)
        binder.shareGiftTitle.show(withAnimation)
    }

    private fun View.show(withAnimation: Boolean) {
        if (withAnimation) {
            animate().alpha(1f)
                .setDuration(400)
                .start()
        } else {
            alpha = 1f
        }
    }
}
