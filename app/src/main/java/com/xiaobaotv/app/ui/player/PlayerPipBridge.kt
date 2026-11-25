package com.xiaobaotv.app.ui.player

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Tiny bridge between the player composable and the hosting activity.
 *
 * The player registers an `enterPip` action while it is on screen; [com.xiaobaotv.app.MainActivity]
 * invokes it from `onUserLeaveHint()` so pressing Home drops into picture-in-picture
 * instead of leaving the app. The activity also mirrors the system PiP state back
 * into [inPip] so the player can hide its chrome while floating.
 */
object PlayerPipBridge {
    @Volatile
    var enterPip: (() -> Boolean)? = null

    val inPip = MutableStateFlow(false)
}
