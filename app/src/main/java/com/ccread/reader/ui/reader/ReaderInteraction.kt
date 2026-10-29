package com.ccread.reader.ui.reader

import com.ccread.reader.domain.model.PageMode

enum class TapZone { LEFT, CENTER, RIGHT }

data class OverlayState(
    val menu: Boolean = false,
    val toc: Boolean = false,
    val settings: Boolean = false,
    val brightness: Boolean = false,
) {
    fun anyVisible(): Boolean = menu || toc || settings || brightness
}

sealed interface ReaderAction {
    data object PreviousPage : ReaderAction
    data object NextPage : ReaderAction
    data class UpdateOverlays(val state: OverlayState) : ReaderAction
}

object ReaderInteraction {

    fun zoneFor(x: Float, width: Float): TapZone = when {
        x < width / 3f -> TapZone.LEFT
        x < width * 2f / 3f -> TapZone.CENTER
        else -> TapZone.RIGHT
    }

    fun onTap(zone: TapZone, mode: PageMode, overlays: OverlayState): ReaderAction = when (zone) {
        TapZone.CENTER -> ReaderAction.UpdateOverlays(OverlayState(menu = !overlays.menu))
        TapZone.LEFT -> if (mode == PageMode.PAGED) ReaderAction.PreviousPage else ReaderAction.UpdateOverlays(overlays)
        TapZone.RIGHT -> if (mode == PageMode.PAGED) ReaderAction.NextPage else ReaderAction.UpdateOverlays(overlays)
    }

    fun onBack(overlays: OverlayState): OverlayState = when {
        overlays.brightness -> overlays.copy(brightness = false)
        overlays.settings -> overlays.copy(settings = false)
        overlays.toc -> overlays.copy(toc = false)
        overlays.menu -> overlays.copy(menu = false)
        else -> overlays
    }
}

enum class PageCommand { PREVIOUS, NEXT }
