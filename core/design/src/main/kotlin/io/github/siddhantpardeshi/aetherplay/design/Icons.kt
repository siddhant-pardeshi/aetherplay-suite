package io.github.siddhantpardeshi.aetherplay.design

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Original AetherPlay iconography (Aurora Precision set). */

val IcCrystal: ImageVector by lazy {
    materialIcon(name = "AetherCrystal") {
        materialPath {
            moveTo(12f, 2f)
            lineTo(19f, 9f)
            lineTo(12f, 22f)
            lineTo(5f, 9f)
            close()
            moveTo(8.4f, 9f)
            lineTo(12f, 5.4f)
            lineTo(15.6f, 9f)
            lineTo(12f, 12.6f)
            close()
            moveTo(7f, 10.4f)
            lineTo(10.6f, 14f)
            lineTo(12f, 18.2f)
            close()
            moveTo(17f, 10.4f)
            lineTo(13.4f, 14f)
            lineTo(12f, 18.2f)
            close()
        }
    }
}

val IcOrbit: ImageVector by lazy {
    materialIcon(name = "AetherOrbit") {
        materialPath {
            moveTo(12f, 8.6f)
            arcTo(3.4f, 3.4f, 0f, false, true, 15.4f, 12f)
            arcTo(3.4f, 3.4f, 0f, false, true, 12f, 15.4f)
            arcTo(3.4f, 3.4f, 0f, false, true, 8.6f, 12f)
            arcTo(3.4f, 3.4f, 0f, false, true, 12f, 8.6f)
            close()
            moveTo(12f, 2.4f)
            arcTo(9.6f, 9.6f, 0f, false, true, 21.6f, 12f)
            arcTo(9.6f, 9.6f, 0f, false, true, 12f, 21.6f)
            arcTo(9.6f, 9.6f, 0f, false, true, 2.4f, 12f)
            arcTo(9.6f, 9.6f, 0f, false, true, 12f, 2.4f)
            close()
            moveTo(12f, 4.4f)
            arcTo(7.6f, 7.6f, 0f, false, false, 4.4f, 12f)
            arcTo(7.6f, 7.6f, 0f, false, true, 12f, 19.6f)
            arcTo(7.6f, 7.6f, 0f, false, true, 19.6f, 12f)
            arcTo(7.6f, 7.6f, 0f, false, true, 12f, 4.4f)
            close()
        }
    }
}

val IcScan: ImageVector by lazy {
    materialIcon(name = "AetherScan") {
        materialPath {
            moveTo(4f, 4f)
            horizontalLineTo(9f)
            verticalLineTo(6f)
            horizontalLineTo(6f)
            verticalLineTo(9f)
            horizontalLineTo(4f)
            close()
            moveTo(15f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(9f)
            horizontalLineTo(18f)
            verticalLineTo(6f)
            horizontalLineTo(15f)
            close()
            moveTo(4f, 15f)
            horizontalLineTo(6f)
            verticalLineTo(18f)
            horizontalLineTo(9f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(18f, 15f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(15f)
            verticalLineTo(18f)
            horizontalLineTo(18f)
            close()
            moveTo(10.9f, 10.9f)
            horizontalLineTo(13.1f)
            verticalLineTo(13.1f)
            horizontalLineTo(10.9f)
            close()
        }
    }
}

val IcBridge: ImageVector by lazy {
    materialIcon(name = "AetherBridge") {
        materialPath {
            moveTo(5f, 9.6f)
            arcTo(2.4f, 2.4f, 0f, false, true, 7.4f, 12f)
            arcTo(2.4f, 2.4f, 0f, false, true, 5f, 14.4f)
            arcTo(2.4f, 2.4f, 0f, false, true, 2.6f, 12f)
            arcTo(2.4f, 2.4f, 0f, false, true, 5f, 9.6f)
            close()
            moveTo(19f, 9.6f)
            arcTo(2.4f, 2.4f, 0f, false, true, 21.4f, 12f)
            arcTo(2.4f, 2.4f, 0f, false, true, 19f, 14.4f)
            arcTo(2.4f, 2.4f, 0f, false, true, 16.6f, 12f)
            arcTo(2.4f, 2.4f, 0f, false, true, 19f, 9.6f)
            close()
            moveTo(8f, 11.1f)
            horizontalLineTo(16f)
            verticalLineTo(12.9f)
            horizontalLineTo(8f)
            close()
        }
    }
}

val IcSpark: ImageVector by lazy {
    materialIcon(name = "AetherSpark") {
        materialPath {
            moveTo(12f, 2f)
            lineTo(13.6f, 8.4f)
            lineTo(20f, 10f)
            lineTo(13.6f, 11.6f)
            lineTo(12f, 18f)
            lineTo(10.4f, 11.6f)
            lineTo(4f, 10f)
            lineTo(10.4f, 8.4f)
            close()
            moveTo(18.5f, 15f)
            lineTo(19.3f, 17.7f)
            lineTo(22f, 18.5f)
            lineTo(19.3f, 19.3f)
            lineTo(18.5f, 22f)
            lineTo(17.7f, 19.3f)
            lineTo(15f, 18.5f)
            lineTo(17.7f, 17.7f)
            close()
        }
    }
}

val IcLayers: ImageVector by lazy {
    materialIcon(name = "AetherLayers") {
        materialPath {
            moveTo(12f, 2f)
            lineTo(22f, 7.5f)
            lineTo(12f, 13f)
            lineTo(2f, 7.5f)
            close()
            moveTo(4.8f, 11.6f)
            lineTo(12f, 15.8f)
            lineTo(19.2f, 11.6f)
            lineTo(22f, 13.2f)
            lineTo(12f, 19f)
            lineTo(2f, 13.2f)
            close()
        }
    }
}
