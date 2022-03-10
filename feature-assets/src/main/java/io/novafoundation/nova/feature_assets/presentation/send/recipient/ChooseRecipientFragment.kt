package io.novafoundation.nova.feature_assets.presentation.send.recipient

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.onDoneClicked
import io.novafoundation.nova.common.utils.onTextChanged
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.common.askPermissionsSafely
import io.novafoundation.nova.feature_assets.presentation.send.phishing.observePhishingCheck
import kotlinx.android.synthetic.main.fragment_choose_recipient.searchRecipientField
import kotlinx.android.synthetic.main.fragment_choose_recipient.searchRecipientFlipper
import kotlinx.android.synthetic.main.fragment_choose_recipient.searchRecipientList
import kotlinx.android.synthetic.main.fragment_choose_recipient.searchRecipientToolbar
import kotlinx.coroutines.launch

private const val INDEX_WELCOME = 0
private const val INDEX_CONTENT = 1
private const val INDEX_EMPTY = 2

class ChooseRecipientFragment : BaseFragment<ChooseRecipientViewModel>(), ChooseRecipientAdapter.RecipientItemHandler {

    companion object {
        private const val PICK_IMAGE_REQUEST = 101
        private const val QR_CODE_IMAGE_TYPE = "image/*"

        private const val PAYLOAD = "ChooseRecipientFragment.PAYLOAD"

        fun getBundle(assetPayload: AssetPayload) = Bundle().apply {
            putParcelable(PAYLOAD, assetPayload)
        }
    }

    private lateinit var adapter: ChooseRecipientAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_choose_recipient, container, false)

    override fun initViews() {
        adapter = ChooseRecipientAdapter(this)

        searchRecipientList.setHasFixedSize(true)
        searchRecipientList.adapter = adapter

        searchRecipientToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }

        searchRecipientToolbar.setRightActionClickListener {
            viewModel.scanClicked()
        }

        searchRecipientField.onDoneClicked {
            viewModel.enterClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .chooseRecipientComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChooseRecipientViewModel) {
        viewModel.screenStateLiveData.observe {
            val index = when (it) {
                State.WELCOME -> INDEX_WELCOME
                State.CONTENT -> INDEX_CONTENT
                State.EMPTY -> INDEX_EMPTY
            }

            searchRecipientFlipper.displayedChild = index
        }

        viewModel.searchResultLiveData.observe(adapter::submitList)

        viewModel.showChooserEvent.observeEvent {
            QrCodeSourceChooserBottomSheet(requireContext(), ::requestCameraPermission, ::selectQrFromGallery)
                .show()
        }

        viewModel.decodeAddressResult.observeEvent {
            searchRecipientField.setText(it)
        }

        viewModel.declinePhishingAddress.observeEvent {
            searchRecipientField.setText("")
        }

        observePhishingCheck(viewModel)

        searchRecipientField.onTextChanged(viewModel::queryChanged)
    }

    private fun requestCameraPermission() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = askPermissionsSafely(Manifest.permission.CAMERA)

            if (result.isSuccess) {
                initiateCameraScanner()
            }
        }
    }

    private fun selectQrFromGallery() {
        val intent = Intent().apply {
            type = QR_CODE_IMAGE_TYPE
            action = Intent.ACTION_GET_CONTENT
        }

        startActivityForResult(Intent.createChooser(intent, getString(R.string.common_options_title)), PICK_IMAGE_REQUEST)
    }

    private fun initiateCameraScanner() {
        val integrator = IntentIntegrator.forSupportFragment(this).apply {
            setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
            setPrompt("")
            setBeepEnabled(false)
        }
        integrator.initiateScan()
    }

    override fun contactClicked(address: String) {
        viewModel.recipientSelected(address)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            viewModel.qrFileChosen(data.data!!)
        } else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            result?.contents?.let {
                viewModel.qrCodeScanned(it)
            }
        }
    }
}
