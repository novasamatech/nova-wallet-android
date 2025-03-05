package io.novafoundation.nova.feature_dapp_impl.presentation.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.recyclerView.dragging.SimpleItemDragHelperCallback
import io.novafoundation.nova.common.utils.recyclerView.dragging.StartDragListener
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.common.DappModel
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.setupRemoveFavouritesConfirmation
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_favorites_dapp.favoritesDappList
import kotlinx.android.synthetic.main.fragment_favorites_dapp.favoritesDappToolbar

class DappFavoritesFragment : BaseBottomSheetFragment<DAppFavoritesViewModel>(), DappDraggableFavoritesAdapter.Handler, StartDragListener {

    @Inject
    protected lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { DappDraggableFavoritesAdapter(imageLoader, this, this) }

    private val itemDragHelper by lazy(LazyThreadSafetyMode.NONE) { ItemTouchHelper(SimpleItemDragHelperCallback(adapter)) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_favorites_dapp, container, false)
    }

    override fun initViews() {
        favoritesDappToolbar.applyStatusBarInsets()
        favoritesDappToolbar.setHomeButtonListener { viewModel.backClicked() }
        favoritesDappList.adapter = adapter
        itemDragHelper.attachToRecyclerView(favoritesDappList)
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .dAppFavoritesComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DAppFavoritesViewModel) {
        setupRemoveFavouritesConfirmation(viewModel.removeFavouriteConfirmationAwaitable)

        viewModel.favoriteDAppsUIFlow.observe {
            adapter.submitList(it)
        }
    }

    override fun onDAppClicked(dapp: DappModel) {
        viewModel.openDApp(dapp)
    }

    override fun onDAppFavoriteClicked(dapp: DappModel) {
        viewModel.onFavoriteClicked(dapp)
    }

    override fun onItemOrderingChanged(dapps: List<DappModel>) {
        viewModel.changeDAppOrdering(dapps)
    }

    override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
        itemDragHelper.startDrag(viewHolder)
    }
}
