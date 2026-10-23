package com.youshu.app.ui.navigation

import android.app.Activity
import android.content.Intent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.youshu.app.ui.screen.camera.CameraScreen
import com.youshu.app.ui.screen.profile.ProfileScreen
import com.youshu.app.ui.screen.settings.SettingsScreen
import com.youshu.app.ui.screen.trash.TrashScreen
import com.youshu.app.ui.warehouse.*
import com.youshu.app.BuildConfig

// Kept for compiled legacy components while their data capabilities are being reused.
internal const val EDIT_RESULT_IMAGE_URI = "edit_result_image_uri"
internal const val EDIT_RESULT_IMAGE_URIS = "edit_result_image_uris"
internal const val EDIT_RESULT_MODE = "edit_result_mode"
internal const val SAVE_RESULT_IMAGE_URI = "save_result_image_uri"
internal const val SAVE_RESULT_IMAGE_URIS = "save_result_image_uris"
internal const val SAVE_RESULT_MODE = "save_result_mode"
private const val WAREHOUSE_PHOTOS = "warehouse_photos"

private data class WarehouseTab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    WarehouseTab(Screen.WarehouseWorkbench.route, "工作台", Icons.Default.Home),
    WarehouseTab(Screen.WarehouseInventory.route, "库存", Icons.Default.Inventory2),
    WarehouseTab(Screen.WarehouseAnalysis.route, "分析", Icons.Default.BarChart),
    WarehouseTab(Screen.Profile.route, "我的", Icons.Default.Person)
)

@Composable
fun AppNavGraph(uiPreview: Boolean = false, photoPreviewLines: List<PhotoReceiveLine> = emptyList(),
    pendingPreviewDrafts: List<StockDraftUi> = emptyList(), outboundPreviewDrafts: List<StockDraftUi> = emptyList()) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val holder: WarehouseUiViewModel = hiltViewModel()
    val state = holder.state
    var photoSamplesConsumed by rememberSaveable { mutableStateOf(false) }
    var pendingSamplesConsumed by rememberSaveable { mutableStateOf(false) }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route == Screen.WarehouseWorkbench.route || route == Screen.WarehouseInventory.route ||
        route == Screen.WarehouseAnalysis.route || route == Screen.Profile.route
    val selectedTab = when {
        route == Screen.Profile.route -> Screen.Profile.route
        route == Screen.WarehouseAnalysis.route -> Screen.WarehouseAnalysis.route
        route == Screen.WarehouseWorkbench.route -> Screen.WarehouseWorkbench.route
        else -> Screen.WarehouseInventory.route
    }
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val back: () -> Unit = { navController.popBackStack() }
    fun openTopLevel(target: String) {
        val concrete = if (target == Screen.WarehouseInventory.route) Screen.WarehouseInventory.createRoute() else target
        navController.navigate(concrete) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun openInventory(filter: StockFilter) {
        navController.navigate(Screen.WarehouseInventory.createRoute(filter.name)) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }
    fun openEntry(direction: EntryDirection, source: EntrySource, product: String? = null, batch: String? = null, draft: String? = null) {
        navController.navigate(Screen.WarehouseEntry.createRoute(direction.name, source.name, product, batch, draft))
    }
    LaunchedEffect(uiPreview) {
        if (uiPreview) {
            if (!state.previewMode) state.enablePreview()
            if (outboundPreviewDrafts.isNotEmpty() && !pendingSamplesConsumed) {
                outboundPreviewDrafts.forEach(state::save)
                pendingSamplesConsumed = true
                navController.navigate(Screen.WarehouseOutbound.route)
            } else if (pendingPreviewDrafts.isNotEmpty() && !pendingSamplesConsumed) {
                pendingPreviewDrafts.forEach(state::save)
                pendingSamplesConsumed = true
                navController.navigate(Screen.WarehousePending.createRoute(EntryDirection.INBOUND.name))
            } else if (photoPreviewLines.isNotEmpty() && !photoSamplesConsumed) openEntry(EntryDirection.INBOUND, EntrySource.PHOTO)
        }
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = WarehousePalette.Background,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        bottomBar = {
            if (showBar && !imeOpen) {
                NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab.route,
                            onClick = { openTopLevel(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WarehousePalette.Teal, selectedTextColor = WarehousePalette.Teal,
                                indicatorColor = WarehousePalette.Mint, unselectedIconColor = WarehousePalette.Muted,
                                unselectedTextColor = WarehousePalette.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.WarehouseWorkbench.route, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(Screen.WarehouseWorkbench.route) {
                WarehouseWorkbenchScreen(
                    onOpenInventory = { openInventory(it) },
                    onReceive = { navController.navigate(Screen.WarehouseReceive.route) },
                    onOutbound = { navController.navigate(Screen.WarehouseOutbound.route) },
                    onCount = { navController.navigate(Screen.WarehouseCount.route) },
                    onPending = { direction -> navController.navigate(Screen.WarehousePending.createRoute(direction.name)) },
                    inboundDraftCount = state.drafts.count { it.direction == EntryDirection.INBOUND },
                    outboundDraftCount = state.drafts.count { it.direction == EntryDirection.OUTBOUND },
                    recentMovements = state.movements.toList(),
                    previewMode = state.previewMode,
                    onUiPreview = if (BuildConfig.DEBUG && !uiPreview) ({
                        context.startActivity(Intent().setClassName(context.packageName, "com.youshu.app.ui.warehouse.WarehouseUiPreviewActivity"))
                    }) else null,
                    onExitPreview = if (uiPreview) ({ (context as? Activity)?.finish() }) else null,
                    data = WorkbenchPreviewData(
                        if (state.previewMode) "青禾便利店 · 后仓" else "本地仓库", state.products.count { it.isNear }.toString(),
                        state.products.count { it.isLow }.toString(), state.drafts.count { it.direction == EntryDirection.INBOUND }.toString(),
                        state.movements.count { it.direction == EntryDirection.OUTBOUND && it.time.toLocalDate() == java.time.LocalDate.now() }
                            .toString() + " 笔",
                        warehousePendingTasks(state.products.toList())
                    )
                )
            }
            composable(Screen.WarehouseInventory.route, arguments = listOf(navArgument("filter") { defaultValue = "ALL" })) { entry ->
                val filter = runCatching { StockFilter.valueOf(entry.arguments?.getString("filter").orEmpty()) }.getOrDefault(StockFilter.ALL)
                WarehouseInventoryScreen(state, filter,
                    onProduct = { navController.navigate(Screen.WarehouseProduct.createRoute(it)) },
                    onReceive = { navController.navigate(Screen.WarehouseReceive.route) })
            }
            composable(Screen.WarehouseProduct.route, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("productId")
                WarehouseProductDetailScreen(state.product(id), state.previewMode, state.movements.filter { it.productId == id }, back,
                    onReceive = { openEntry(EntryDirection.INBOUND, EntrySource.MANUAL, id) },
                    onOutbound = { batch -> openEntry(EntryDirection.OUTBOUND, EntrySource.MANUAL, id, batch) },
                    onTransfer = { id?.let { navController.navigate(Screen.WarehouseTransfer.createRoute(it)) } })
            }
            listOf(Screen.WarehouseReceive to EntryDirection.INBOUND, Screen.WarehouseOutbound to EntryDirection.OUTBOUND).forEach { (screen, direction) ->
                composable(screen.route) {
                    WarehouseTransactionHomeScreen(direction, state, back,
                        onSource = { openEntry(direction, it) },
                        onPending = { navController.navigate(Screen.WarehousePending.createRoute(direction.name)) },
                        onEdit = { id -> state.draft(id)?.let { openEntry(it.direction, it.source, it.productId, it.batchId, it.id) } })
                }
            }
            composable(Screen.WarehouseEntry.route, arguments = listOf(
                navArgument("direction") { type = NavType.StringType }, navArgument("source") { type = NavType.StringType },
                navArgument("product") { defaultValue = "" }, navArgument("batch") { defaultValue = "" }, navArgument("draft") { defaultValue = "" }
            )) { entry ->
                val direction = EntryDirection.valueOf(entry.arguments!!.getString("direction")!!)
                val source = EntrySource.valueOf(entry.arguments!!.getString("source")!!)
                val draftId = entry.arguments?.getString("draft")?.takeIf { it.isNotBlank() }
                val existing = state.draft(draftId)
                val photoReceive = direction == EntryDirection.INBOUND && source == EntrySource.PHOTO
                val photos by entry.savedStateHandle.getStateFlow(WAREHOUSE_PHOTOS,
                    if (photoReceive || direction == EntryDirection.OUTBOUND) emptyList<String>() else existing?.imageUris.orEmpty()).collectAsState()
                val onSaved: () -> Unit = {
                    navController.navigate(Screen.WarehousePending.createRoute(direction.name)) {
                        popUpTo(Screen.WarehouseEntry.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                if (photoReceive) WarehousePhotoReceiveScreen(
                    state = state, existing = existing, incomingPhotos = photos,
                    onPhotosConsumed = { entry.savedStateHandle[WAREHOUSE_PHOTOS] = emptyList<String>() },
                    onBack = back, onCamera = { navController.navigate(Screen.Camera.route) },
                    onSaveDrafts = holder::saveDrafts, onSaved = onSaved,
                    demoLines = if (uiPreview && !photoSamplesConsumed) photoPreviewLines else emptyList(),
                    onDemoConsumed = { photoSamplesConsumed = true }
                ) else if (direction == EntryDirection.OUTBOUND) WarehouseOutboundEntryScreen(
                    state, source, entry.arguments?.getString("product")?.takeIf { it.isNotBlank() },
                    entry.arguments?.getString("batch")?.takeIf { it.isNotBlank() }, draftId, photos,
                    onPhotosConsumed = { entry.savedStateHandle[WAREHOUSE_PHOTOS] = emptyList<String>() },
                    onBack = back, onCamera = { navController.navigate(Screen.Camera.route) },
                    onSaveDraft = holder::saveDraft, onSaved = onSaved
                ) else WarehouseStockFormScreen(
                    state, direction, source,
                    entry.arguments?.getString("product")?.takeIf { it.isNotBlank() },
                    entry.arguments?.getString("batch")?.takeIf { it.isNotBlank() }, draftId, photos, back,
                    onCamera = { navController.navigate(Screen.Camera.route) },
                    onSaveDraft = { draft, done -> holder.saveDraft(draft, done) },
                    onSaved = onSaved
                )
            }
            composable(Screen.WarehousePending.route, arguments = listOf(navArgument("direction") { type = NavType.StringType })) { entry ->
                val direction = EntryDirection.valueOf(entry.arguments!!.getString("direction")!!)
                WarehousePendingScreen(state, direction, back,
                    onEdit = { id -> state.draft(id)?.let { openEntry(it.direction, it.source, it.productId, it.batchId, it.id) } },
                    onReview = holder::reviewDraft,
                    onDelete = holder::deleteDraft,
                    onConfirm = holder::confirm,
                    onContinue = { navController.navigate(if (direction == EntryDirection.INBOUND) Screen.WarehouseReceive.route else Screen.WarehouseOutbound.route) },
                    onReload = holder::reloadPendingDrafts, onReviewAll = holder::reviewDrafts,
                    onSaveDraft = holder::saveDraft)
            }
            composable(Screen.WarehouseTransfer.route, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                WarehouseTransferScreen(state.product(entry.arguments?.getString("productId")), back, state.previewMode,
                    onConfirm = holder::transferBatch)
            }
            composable(Screen.WarehouseCount.route) {
                WarehouseCountScreen(state, back, onConfirm = holder::countBatch)
            }
            composable(Screen.Camera.route) {
                CameraScreen(onBack = back, onSkipPhoto = back,
                    actionColorStart = WarehousePalette.Teal, actionColorEnd = WarehousePalette.TealDark,
                    confirmationLabel = "识别照片", onPhotoTaken = { uris ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        val old = handle.get<List<String>>(WAREHOUSE_PHOTOS).orEmpty()
                        handle[WAREHOUSE_PHOTOS] = (old + uris.map { it.toString() }).distinct()
                    }
                    navController.popBackStack()
                })
            }
            composable(Screen.WarehouseAnalysis.route) {
                WarehouseAnalysisScreen(state, onProduct = {
                    navController.navigate(Screen.WarehouseAnalysisProduct.createRoute(it))
                })
            }
            composable(Screen.WarehouseAnalysisProduct.route, arguments = listOf(navArgument("productId") { type = NavType.StringType })) { entry ->
                WarehouseAnalysisDetailScreen(
                    product = state.product(entry.arguments?.getString("productId")),
                    movements = state.movements.toList(), drafts = state.drafts.toList(),
                    preview = state.previewMode, onBack = back,
                    onSavePolicy = holder::updateReplenishmentPolicy
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onOpenExpiry = { openInventory(StockFilter.NEAR) },
                    onOpenTrash = { navController.navigate(Screen.Trash.route) },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    sessionDraftCount = state.drafts.size,
                    productCount = state.products.count { it.availableQuantity > 0 },
                    expiringCount = state.products.count { it.isNear },
                    previewMode = state.previewMode)
            }
            composable(Screen.Trash.route) { TrashScreen(onBack = back) }
            composable(Screen.Settings.route) { SettingsScreen(onBack = back) }
        }
    }
}
