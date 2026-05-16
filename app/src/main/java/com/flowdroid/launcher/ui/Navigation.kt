package com.flowdroid.launcher.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.flowdroid.launcher.ui.screens.*

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Containers : Screen("containers")
    object Hidden     : Screen("hidden")
    object Folder     : Screen("folder/{folderId}") {
        fun createRoute(id: Long) = "folder/$id"
    }
}

@Composable
fun FlowDroidNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToFolder     = { navController.navigate(Screen.Folder.createRoute(it)) },
                onNavigateToContainers = { navController.navigate(Screen.Containers.route) },
                onNavigateToHidden     = { navController.navigate(Screen.Hidden.route) },
            )
        }

        composable(Screen.Containers.route) {
            ContainersScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Hidden.route) {
            HiddenAppsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Folder.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments!!.getLong("folderId")
            FolderScreen(
                folderId = folderId,
                onBack   = { navController.popBackStack() },
            )
        }
    }
}
