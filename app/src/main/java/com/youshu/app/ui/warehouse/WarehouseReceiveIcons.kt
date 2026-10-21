package com.youshu.app.ui.warehouse

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Native vector illustrations for the receive workflow; shared colors match the dashboard. */
internal object WarehouseReceiveIcons {
    private val white = SolidColor(Color.White)

    val Store by lazy {
        icon("ReceiveStore") {
            path(fill = teal()) {
                moveTo(8f, 18f); lineTo(40f, 18f); lineTo(40f, 41f)
                curveTo(40f, 43f, 38f, 45f, 36f, 45f); lineTo(12f, 45f)
                curveTo(10f, 45f, 8f, 43f, 8f, 41f); close()
            }
            path(fill = teal()) {
                moveTo(12f, 5f); lineTo(36f, 5f); lineTo(44f, 16f)
                curveTo(44f, 23f, 35f, 25f, 34f, 18f)
                curveTo(32f, 25f, 24f, 25f, 24f, 18f)
                curveTo(23f, 25f, 15f, 25f, 14f, 18f)
                curveTo(12f, 25f, 4f, 23f, 4f, 16f); close()
            }
            path(fill = white) {
                moveTo(14f, 30f); curveTo(14f, 27f, 22f, 27f, 22f, 30f)
                lineTo(22f, 45f); lineTo(14f, 45f); close()
            }
            path(stroke = white, strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(28f, 30f); lineTo(34f, 30f)
                moveTo(28f, 36f); lineTo(34f, 36f)
            }
        }
    }

    val Camera by lazy {
        icon("ReceiveCamera") {
            path(fill = teal()) {
                moveTo(8f, 15f); lineTo(15f, 15f); lineTo(19f, 8f); lineTo(29f, 8f)
                lineTo(33f, 15f); lineTo(39f, 15f)
                curveTo(43f, 15f, 45f, 17f, 45f, 21f); lineTo(45f, 39f)
                curveTo(45f, 43f, 43f, 45f, 39f, 45f); lineTo(8f, 45f)
                curveTo(4f, 45f, 2f, 43f, 2f, 39f); lineTo(2f, 21f)
                curveTo(2f, 17f, 4f, 15f, 8f, 15f); close()
            }
            path(stroke = white, strokeLineWidth = 3.2f) {
                moveTo(34f, 29f)
                curveTo(34f, 35f, 29.5f, 39.5f, 23.5f, 39.5f)
                curveTo(17.5f, 39.5f, 13f, 35f, 13f, 29f)
                curveTo(13f, 23f, 17.5f, 18.5f, 23.5f, 18.5f)
                curveTo(29.5f, 18.5f, 34f, 23f, 34f, 29f); close()
            }
            path(stroke = SolidColor(Color(0xFF16AD91)), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round) {
                moveTo(37f, 7f); lineTo(38f, 2f)
                moveTo(43f, 10f); lineTo(47f, 7f)
            }
        }
    }

    val Voice by lazy {
        icon("ReceiveVoice") {
            path(fill = gradient(0xFF4DAAFF, 0xFF0879EF)) {
                moveTo(24f, 3f); curveTo(29f, 3f, 32f, 6f, 32f, 11f)
                lineTo(32f, 24f); curveTo(32f, 35f, 16f, 35f, 16f, 24f)
                lineTo(16f, 11f); curveTo(16f, 6f, 19f, 3f, 24f, 3f); close()
            }
            path(stroke = gradient(0xFF50B3FF, 0xFF0B80EE), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(10f, 23f); curveTo(10f, 42f, 38f, 42f, 38f, 23f)
                moveTo(24f, 38f); lineTo(24f, 45f)
                moveTo(17f, 46f); lineTo(31f, 46f)
                moveTo(5f, 17f); curveTo(2f, 21f, 2f, 26f, 4f, 30f)
                moveTo(43f, 17f); curveTo(46f, 21f, 46f, 26f, 44f, 30f)
            }
        }
    }

    val Record by lazy {
        icon("ReceiveRecord") {
            path(fill = teal()) {
                moveTo(12f, 4f); lineTo(36f, 4f); curveTo(39f, 4f, 40f, 6f, 40f, 9f)
                lineTo(40f, 42f); lineTo(8f, 42f); lineTo(8f, 9f)
                curveTo(8f, 6f, 9f, 4f, 12f, 4f); close()
            }
            path(stroke = white, strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(16f, 13f); lineTo(31f, 13f)
                moveTo(16f, 22f); lineTo(27f, 22f)
                moveTo(16f, 31f); lineTo(23f, 31f)
                moveTo(32f, 33f); lineTo(32f, 35f)
            }
        }
    }

    val Review by lazy {
        icon("ReceiveReview") {
            path(fill = teal()) {
                moveTo(10f, 5f); lineTo(29f, 5f); lineTo(39f, 15f); lineTo(39f, 42f)
                lineTo(10f, 42f); close()
            }
            path(fill = SolidColor(Color(0xFFB8E9DF))) {
                moveTo(29f, 5f); lineTo(29f, 15f); lineTo(39f, 15f); close()
            }
            path(stroke = white, strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(17f, 18f); lineTo(26f, 18f)
                moveTo(17f, 26f); lineTo(29f, 26f)
                moveTo(17f, 34f); lineTo(22f, 34f)
            }
            path(stroke = white, strokeLineWidth = 6.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(28f, 37f); lineTo(34f, 43f); lineTo(45f, 31f)
            }
            path(stroke = teal(), strokeLineWidth = 3.4f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(28f, 37f); lineTo(34f, 43f); lineTo(45f, 31f)
            }
        }
    }

    val Stock by lazy {
        icon("ReceiveStock") {
            path(fill = gradient(0xFF1BB699, 0xFF00957E)) {
                moveTo(24f, 3f); lineTo(44f, 13f); lineTo(24f, 23f); lineTo(4f, 13f); close()
            }
            path(fill = gradient(0xFF17B79B, 0xFF007C6D)) {
                moveTo(4f, 16f); lineTo(22.5f, 25.5f); lineTo(22.5f, 46f); lineTo(4f, 36.5f); close()
            }
            path(fill = gradient(0xFF24CAA6, 0xFF009A80)) {
                moveTo(25.5f, 25.5f); lineTo(44f, 16f); lineTo(44f, 36.5f); lineTo(25.5f, 46f); close()
            }
        }
    }

    val Pending by lazy {
        icon("ReceivePending") {
            path(fill = gradient(0xFFFFB526, 0xFFFF8108)) {
                moveTo(10f, 5f); lineTo(38f, 5f); curveTo(42f, 5f, 44f, 7f, 44f, 11f)
                lineTo(44f, 37f); curveTo(44f, 41f, 42f, 43f, 38f, 43f)
                lineTo(10f, 43f); curveTo(6f, 43f, 4f, 41f, 4f, 37f)
                lineTo(4f, 11f); curveTo(4f, 7f, 6f, 5f, 10f, 5f); close()
            }
            path(stroke = white, strokeLineWidth = 2.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11f, 17f); lineTo(14f, 20f); lineTo(19f, 14f)
                moveTo(25f, 17f); lineTo(36f, 17f)
                moveTo(11f, 30f); lineTo(14f, 33f); lineTo(19f, 27f)
                moveTo(25f, 30f); lineTo(36f, 30f)
            }
        }
    }

    private fun teal() = gradient(0xFF1AB899, 0xFF008271)
    private fun gradient(start: Long, end: Long) = Brush.linearGradient(
        listOf(Color(start), Color(end)), start = Offset(6f, 4f), end = Offset(42f, 46f))
    private fun icon(name: String, content: ImageVector.Builder.() -> Unit) =
        ImageVector.Builder(name, 48.dp, 48.dp, 48f, 48f).apply(content).build()
}
