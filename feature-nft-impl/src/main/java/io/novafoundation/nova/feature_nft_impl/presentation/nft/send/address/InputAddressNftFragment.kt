package io.novafoundation.nova.feature_assets.presentation.send.amount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import coil.ImageLoader
import coil.load
import coil.transform.RoundedCornersTransformation
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.impl.observeValidations
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_account_api.presenatation.actions.setupExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupExternalAccounts
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.R
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureComponent
import io.novafoundation.nova.feature_nft_impl.presentation.NftPayload
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.setupFeeLoading
import kotlinx.android.synthetic.main.fragment_input_address_nft.chooseAmountContainer
import kotlinx.android.synthetic.main.fragment_input_address_nft.closeAction
import kotlinx.android.synthetic.main.fragment_input_address_nft.inputAddressFee
import kotlinx.android.synthetic.main.fragment_input_address_nft.inputAddressNext
import kotlinx.android.synthetic.main.fragment_input_address_nft.inputAddressRecipient
import kotlinx.android.synthetic.main.fragment_input_address_nft.inputAddressWallet
import kotlinx.android.synthetic.main.fragment_input_address_nft.nftChain
import kotlinx.android.synthetic.main.fragment_input_address_nft.nftName
import kotlinx.android.synthetic.main.fragment_input_address_nft.nftThumb
import kotlinx.android.synthetic.main.fragment_nft_details.nftDetailsMedia
import javax.inject.Inject

private const val KEY_NFT_PAYLOAD = "KEY_NFT_PAYLOAD"

class InputAddressNftFragment : BaseFragment<InputAddressNftViewModel>() {

    companion object {

        fun getBundle(nftPayload: NftPayload): Bundle {
            return bundleOf(
                KEY_NFT_PAYLOAD to nftPayload
            )
        }
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_input_address_nft, container, false)

    override fun initViews() {
        chooseAmountContainer.applyStatusBarInsets(false)
        inputAddressNext.prepareForProgress(viewLifecycleOwner)
        inputAddressNext.setOnClickListener { viewModel.nextClicked() }

        closeAction.setOnClickListener { viewModel.backClicked()  }

        inputAddressWallet.background = getRoundedCornerDrawable(cornerSizeDp = 8).withRippleMask(getRippleMask(cornerSizeDp = 8))
        inputAddressWallet.setOnClickListener { viewModel.selectRecipientWallet() }
    }

    override fun inject() {
        FeatureUtils.getFeature<NftFeatureComponent>(this, NftFeatureApi::class.java)
            .inputAddressNftComponentFactory()
            .create(this, argument(KEY_NFT_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: InputAddressNftViewModel) {
        setupExternalActions(viewModel)

        observeValidations(viewModel)

        setupFeeLoading(viewModel.originFeeMixin, inputAddressFee)

        setupAddressInput(viewModel.addressInputMixin, inputAddressRecipient)
        setupExternalAccounts(viewModel.addressInputMixin, inputAddressRecipient)

        viewModel.isSelectAddressAvailable.observe {
            inputAddressWallet.isInvisible = !it
        }
        viewModel.nftName.observe {
            nftName.text = it
        }
        viewModel.chainUI.observe { nftChain.setChain(it) }

        viewModel.nftMedia.observe { media ->
            nftThumb.load(media, imageLoader) {
                transformations(RoundedCornersTransformation(4.dpF(nftThumb.context)))
                placeholder(R.drawable.nft_media_progress)
                error(R.drawable.nft_media_error)
                fallback(R.drawable.nft_media_error)
            }
        }
    }
}
