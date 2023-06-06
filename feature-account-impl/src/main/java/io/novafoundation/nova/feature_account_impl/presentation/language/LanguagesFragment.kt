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
import kotlinx.android.synthetic.main.fragment_languages.languagesList
import kotlinx.android.synthetic.main.fragment_languages.novaToolbar

class LanguagesFragment : BaseFragment<LanguagesViewModel>(), LanguagesAdapter.LanguagesItemHandler {

    private lateinit var adapter: LanguagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_languages, container, false)

    override fun initViews() {
        adapter = LanguagesAdapter(this)

        languagesList.setHasFixedSize(true)
        languagesList.adapter = adapter

        novaToolbar.setHomeButtonListener {
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
