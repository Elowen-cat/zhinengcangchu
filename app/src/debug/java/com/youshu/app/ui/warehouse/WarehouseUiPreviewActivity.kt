package com.youshu.app.ui.warehouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.youshu.app.ui.navigation.AppNavGraph
import com.youshu.app.ui.theme.YouShuTheme
import dagger.hilt.android.AndroidEntryPoint

// Debug-only, separately launched preview. Never seeds or writes the Room database.
@AndroidEntryPoint
class WarehouseUiPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val samples = if (intent.getBooleanExtra("photo_preview", false)) photoReceivePreviewSamples(packageName) else emptyList()
        setContent { YouShuTheme { AppNavGraph(uiPreview = true, photoPreviewLines = samples) } }
    }
}
