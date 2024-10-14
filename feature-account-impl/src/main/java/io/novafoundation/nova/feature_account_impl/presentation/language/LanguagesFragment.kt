package io.novafoundation.nova.feature_account_impl.presentation.language

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseActivity
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageModel

class LanguagesFragment : BaseFragment<LanguagesViewModel, FragmentLanguagesBinding>(), LanguagesAdapter.LanguagesItemHandler {

    override val binder by viewBinding(FragmentLanguagesBinding::bind)

    private lateinit var adapter: LanguagesAdapter

    override fun initViews() {
        adapter = LanguagesAdapter(this)

        binder.languagesList.setHasFixedSize(true)
        binder.languagesList.adapter = adapter

        binder.novaToolbar.setHomeButtonListener {
            viewModel.backClicked()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(
            requireContext(),
            AccountFeatureApi::class.java
        )
            .languagesComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: LanguagesViewModel) {
        adapter.submitList(viewModel.languagesModels)

        viewModel.selectedLanguageLiveData.observe(adapter::updateSelectedLanguage)

        viewModel.languageChangedEvent.observeEvent {
            (activity as BaseActivity<*>).changeLanguage()
        }
    }

    override fun checkClicked(languageModel: LanguageModel) {
        viewModel.selectLanguageClicked(languageModel)
    }
}
