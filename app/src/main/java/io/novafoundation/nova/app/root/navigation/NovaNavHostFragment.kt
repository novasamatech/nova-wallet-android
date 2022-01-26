package io.novafoundation.nova.app.root.navigation

import android.annotation.SuppressLint
import androidx.navigation.NavController
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.navigation.navigators.AddFragmentNavigator

class NovaNavHostFragment : NavHostFragment() {

    @SuppressLint("MissingSuperCall")
    override fun onCreateNavController(navController: NavController) {
        navController.navigatorProvider.addNavigator(DialogFragmentNavigator(requireContext(), childFragmentManager))

        val addFragmentNavigator = AddFragmentNavigator(requireContext(), childFragmentManager, R.id.navHost)

        navController.navigatorProvider.addNavigator(addFragmentNavigator)
    }
}
