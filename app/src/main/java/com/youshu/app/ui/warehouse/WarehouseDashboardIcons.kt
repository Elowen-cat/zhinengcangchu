package com.youshu.app.ui.warehouse

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small, scalable illustrations shared by dashboard statistics and actions. */
internal object WarehouseDashboardIcons {
    val Expiring by lazy {
        icon("Expiring") {
            path(fill = gradient(0xFFFFC052, 0xFFFF8B06)) {
                moveTo(20.5f, 6f)
                curveTo(22f, 3.3f, 26f, 3.3f, 27.5f, 6f)
                lineTo(45f, 37f)
                curveTo(46.6f, 40f, 44.5f, 43f, 41f, 43f)
                lineTo(7f, 43f)
                curveTo(3.5f, 43f, 1.4f, 40f, 3f, 37f)
                close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 3.7f, strokeLineCap = StrokeCap.Round) {
                moveTo(24f, 17f); lineTo(24f, 29f)
                moveTo(24f, 36f); lineTo(24f, 36.4f)
            }
        }
    }

    val LowStock by lazy {
        icon("LowStock") {
            path(fill = gradient(0xFFFF8284, 0xFFED424D)) {
                moveTo(24f, 3f); lineTo(44f, 13f); lineTo(24f, 23f); lineTo(4f, 13f); close()
            }
            path(fill = gradient(0xFFFF6671, 0xFFDE3548)) {
                moveTo(4f, 16f); lineTo(22.5f, 25.5f); lineTo(22.5f, 46f); lineTo(4f, 36.5f); close()
            }
            path(fill = gradient(0xFFFF7479, 0xFFF04454)) {
                moveTo(25.5f, 25.5f); lineTo(44f, 16f); lineTo(44f, 36.5f); lineTo(25.5f, 46f); close()
            }
        }
    }

    val Delivery by lazy {
        icon("Delivery") {
            path(fill = gradient(0xFF6598EF, 0xFF2F68C7)) {
                moveTo(7f, 8f); lineTo(26f, 8f)
                curveTo(28f, 8f, 29f, 9f, 29f, 11f)
                lineTo(29f, 37f); lineTo(4f, 37f); lineTo(4f, 11f)
                curveTo(4f, 9f, 5f, 8f, 7f, 8f); close()
            }
            path(fill = gradient(0xFF80AFF9, 0xFF376EC5)) {
                moveTo(31f, 19f); lineTo(37f, 19f); lineTo(45f, 28f)
                lineTo(45f, 37f); lineTo(31f, 37f); close()
            }
            path(fill = SolidColor(Color.White.copy(alpha = 0.93f))) {
                moveTo(33f, 22f); lineTo(36f, 22f); lineTo(41.5f, 28f); lineTo(33f, 28f); close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 12f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 38f); lineTo(12.1f, 38f)
                moveTo(36f, 38f); lineTo(36.1f, 38f)
            }
            path(stroke = SolidColor(Color(0xFF396FC4)), strokeLineWidth = 8f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 38f); lineTo(12.1f, 38f)
                moveTo(36f, 38f); lineTo(36.1f, 38f)
            }
        }
    }

    val OutOfStock by lazy {
        icon("OutOfStock") {
            path(fill = gradient(0xFFD6556B, 0xFFB52C44)) {
                moveTo(24f, 3f); lineTo(44f, 13f); lineTo(24f, 23f); lineTo(4f, 13f); close()
            }
            path(fill = gradient(0xFFB6314C, 0xFF851229)) {
                moveTo(4f, 16f); lineTo(22.5f, 25.5f); lineTo(22.5f, 46f); lineTo(4f, 36.5f); close()
            }
            path(fill = gradient(0xFFCD455D, 0xFFA31E37)) {
                moveTo(25.5f, 25.5f); lineTo(44f, 16f); lineTo(44f, 36.5f); lineTo(25.5f, 46f); close()
            }
        }
    }

    val OutboundTrend by lazy {
        icon("OutboundTrend") {
            path(stroke = gradient(0xFF67D6B8, 0xFF0C9E7A), strokeLineWidth = 6f, strokeLineCap = StrokeCap.Round) {
                moveTo(10f, 31f); lineTo(10f, 41f)
                moveTo(24f, 20f); lineTo(24f, 41f)
                moveTo(38f, 8f); lineTo(38f, 41f)
            }
        }
    }

    val Receive by lazy {
        icon("Receive") {
            path(fill = gradient(0xFF24BAA1, 0xFF007D70)) {
                moveTo(7f, 20f); lineTo(41f, 20f); lineTo(41f, 40f)
                curveTo(41f, 43f, 39f, 45f, 36f, 45f)
                lineTo(12f, 45f); curveTo(9f, 45f, 7f, 43f, 7f, 40f); close()
            }
            path(fill = gradient(0xFF4AD3B6, 0xFF11957F)) {
                moveTo(7f, 18f); lineTo(15f, 6f); lineTo(25f, 6f); lineTo(19f, 18f); close()
                moveTo(23f, 18f); lineTo(29f, 6f); lineTo(35f, 6f); lineTo(43f, 18f); close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(24f, 24f); lineTo(24f, 37f)
                moveTo(18f, 31f); lineTo(24f, 37f); lineTo(30f, 31f)
            }
        }
    }

    val Dispatch by lazy {
        icon("Dispatch") {
            path(fill = gradient(0xFF669BEF, 0xFF2964C4)) {
                moveTo(6f, 21f); lineTo(21f, 6f)
                curveTo(22.7f, 4.3f, 25.3f, 4.3f, 27f, 6f)
                lineTo(42f, 21f); lineTo(42f, 40f)
                curveTo(42f, 43f, 40f, 45f, 37f, 45f)
                lineTo(11f, 45f); curveTo(8f, 45f, 6f, 43f, 6f, 40f); close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(24f, 36f); lineTo(24f, 20f)
                moveTo(17f, 27f); lineTo(24f, 20f); lineTo(31f, 27f)
            }
        }
    }

    val Transfer by lazy {
        icon("Transfer") {
            path(stroke = gradient(0xFFAE68DE, 0xFF792FB2), strokeLineWidth = 5.5f, strokeLineCap = StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(10f, 14f); lineTo(33f, 14f)
                curveTo(39f, 14f, 40f, 18f, 40f, 22f)
                moveTo(16f, 7f); lineTo(9f, 14f); lineTo(16f, 21f)
                moveTo(38f, 34f); lineTo(15f, 34f)
                curveTo(9f, 34f, 8f, 30f, 8f, 26f)
                moveTo(32f, 27f); lineTo(39f, 34f); lineTo(32f, 41f)
            }
        }
    }

    val Count by lazy {
        icon("Count") {
            path(fill = gradient(0xFFA67AC3, 0xFF744298)) {
                moveTo(12f, 9f); lineTo(36f, 9f)
                curveTo(39f, 9f, 41f, 11f, 41f, 14f)
                lineTo(41f, 40f); curveTo(41f, 43f, 39f, 45f, 36f, 45f)
                lineTo(12f, 45f); curveTo(9f, 45f, 7f, 43f, 7f, 40f)
                lineTo(7f, 14f); curveTo(7f, 11f, 9f, 9f, 12f, 9f); close()
            }
            path(fill = SolidColor(Color(0xFFEFE3F8))) {
                moveTo(18f, 3f); lineTo(30f, 3f); lineTo(30f, 11f); lineTo(18f, 11f); close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(21f, 20f); lineTo(33f, 20f)
                moveTo(21f, 28f); lineTo(33f, 28f)
                moveTo(21f, 36f); lineTo(33f, 36f)
                moveTo(14.5f, 20f); lineTo(14.7f, 20f)
                moveTo(14.5f, 28f); lineTo(14.7f, 28f)
                moveTo(14.5f, 36f); lineTo(14.7f, 36f)
            }
        }
    }

    private fun gradient(start: Long, end: Long) = Brush.linearGradient(
        listOf(Color(start), Color(end)), start = Offset(8f, 4f), end = Offset(40f, 46f))

    private fun icon(name: String, content: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 48.dp, 48.dp, 48f, 48f).apply(content).build()
}
