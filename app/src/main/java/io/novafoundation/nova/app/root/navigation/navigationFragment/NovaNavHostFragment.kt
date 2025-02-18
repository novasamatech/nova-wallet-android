package io.novafoundation.nova.app.root.navigation.navigationFragment

import android.annotation.SuppressLint
import androidx.navigation.NavController
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import io.novafoundation.nova.app.root.navigation.navigators.AddFragmentNavigator

abstract class NovaNavHostFragment : NavHostFragment() {

    abstract val containerId: Int

    @SuppressLint("MissingSuperCall")
    override fun onCreateNavController(navController: NavController) {
        navController.navigatorProvider.addNavigator(DialogFragmentNavigator(requireContext(), childFragmentManager))
        val addFragmentNavigator = AddFragmentNavigator(requireContext(), childFragmentManager, containerId)

        navController.navigatorProvider.addNavigator(addFragmentNavigator)
    }
}
