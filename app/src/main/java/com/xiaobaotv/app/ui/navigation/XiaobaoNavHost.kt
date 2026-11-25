package com.xiaobaotv.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.xiaobaotv.app.ui.category.CategoryScreen
import com.xiaobaotv.app.ui.detail.DetailScreen
import com.xiaobaotv.app.ui.history.HistoryScreen
import com.xiaobaotv.app.ui.home.HomeScreen
import com.xiaobaotv.app.ui.player.PlayerScreen
import com.xiaobaotv.app.ui.profile.SettingsScreen
import com.xiaobaotv.app.ui.search.SearchScreen
import com.xiaobaotv.app.ui.theme.Motion

object Routes {
    const val HOME = "home"
    const val CATEGORY = "category"
    const val SEARCH = "search"
    const val HISTORY = "history"
    const val DETAIL = "detail/{vodId}"
    const val PLAYER = "player/{vodId}/{episodeNum}"
    const val SETTINGS = "settings"

    fun detail(vodId: Int) = "detail/$vodId"
    fun player(vodId: Int, episodeNum: Int = 1) = "player/$vodId/$episodeNum"
}

private const val TAB_ENTER_MS = 240
private const val DETAIL_ENTER_MS = 360
private const val PLAYER_ENTER_MS = 320

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterFor(route: String?): EnterTransition =
    when (route) {
        Routes.DETAIL -> fadeIn(tween(220)) +
            slideInVertically(
                animationSpec = tween(DETAIL_ENTER_MS, easing = Motion.EmphasizedDecelerate)
            ) { fullHeight -> fullHeight / 6 } +
            scaleIn(
                animationSpec = tween(DETAIL_ENTER_MS, easing = Motion.EmphasizedDecelerate),
                initialScale = Motion.DetailEnterScale
            )

        Routes.PLAYER -> fadeIn(tween(200)) +
            slideInVertically(
                animationSpec = tween(PLAYER_ENTER_MS, easing = Motion.EmphasizedDecelerate)
            ) { fullHeight -> fullHeight }

        Routes.SETTINGS -> fadeIn(tween(200)) +
            slideInHorizontally(
                animationSpec = tween(300, easing = Motion.Standard)
            ) { fullWidth -> fullWidth / 5 }

        else -> fadeIn(tween(TAB_ENTER_MS)) +
            scaleIn(animationSpec = tween(TAB_ENTER_MS), initialScale = 0.985f)
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitFor(route: String?): ExitTransition =
    when (route) {
        Routes.DETAIL, Routes.PLAYER, Routes.SETTINGS -> fadeOut(tween(160))
        else -> fadeOut(tween(150)) + scaleOut(animationSpec = tween(200), targetScale = 0.99f)
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterFor(route: String?): EnterTransition =
    when (route) {
        Routes.DETAIL -> fadeIn(tween(260)) +
            scaleIn(
                animationSpec = tween(320, easing = Motion.EmphasizedDecelerate),
                initialScale = 0.965f
            )

        Routes.SETTINGS -> fadeIn(tween(220)) +
            slideInHorizontally(
                animationSpec = tween(280, easing = Motion.Standard)
            ) { fullWidth -> -fullWidth / 6 }

        else -> fadeIn(tween(220))
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitFor(route: String?): ExitTransition =
    when (route) {
        Routes.DETAIL -> fadeOut(tween(220)) +
            slideOutVertically(
                animationSpec = tween(320, easing = Motion.EmphasizedAccelerate)
            ) { fullHeight -> fullHeight / 4 } +
            scaleOut(
                animationSpec = tween(320, easing = Motion.EmphasizedAccelerate),
                targetScale = 0.95f
            )

        Routes.PLAYER -> fadeOut(tween(220)) +
            slideOutVertically(
                animationSpec = tween(300, easing = Motion.EmphasizedAccelerate)
            ) { fullHeight -> fullHeight }

        Routes.SETTINGS -> fadeOut(tween(200)) +
            slideOutHorizontally(
                animationSpec = tween(260, easing = Motion.EmphasizedAccelerate)
            ) { fullWidth -> fullWidth / 5 }

        else -> fadeOut(tween(170))
    }

@Composable
fun XiaobaoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { enterFor(targetState.destination.route) },
        exitTransition = { exitFor(targetState.destination.route) },
        popEnterTransition = { popEnterFor(initialState.destination.route) },
        popExitTransition = { popExitFor(initialState.destination.route) }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onVodClick = { vodId -> navController.navigate(Routes.detail(vodId)) },
                onPlayClick = { vodId -> navController.navigate(Routes.player(vodId, 1)) },
                onResumeClick = { vodId, episodeNum ->
                    navController.navigate(Routes.player(vodId, episodeNum))
                },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.CATEGORY) {
            CategoryScreen(onVodClick = { vodId -> navController.navigate(Routes.detail(vodId)) })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onVodClick = { vodId -> navController.navigate(Routes.detail(vodId)) })
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onVodClick = { vodId -> navController.navigate(Routes.detail(vodId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("vodId") { type = NavType.IntType })
        ) { backStackEntry ->
            val vodId = backStackEntry.arguments?.getInt("vodId") ?: 0
            DetailScreen(
                vodId = vodId,
                onPlayClick = { id, ep -> navController.navigate(Routes.player(id, ep)) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("vodId") { type = NavType.IntType },
                navArgument("episodeNum") { type = NavType.IntType; defaultValue = 1 }
            )
        ) { backStackEntry ->
            val vodId = backStackEntry.arguments?.getInt("vodId") ?: 0
            val episodeNum = backStackEntry.arguments?.getInt("episodeNum") ?: 1
            PlayerScreen(
                vodId = vodId,
                episodeIndex = episodeNum - 1,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
