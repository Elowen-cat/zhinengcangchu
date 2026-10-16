package com.youshu.app.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    data object WarehouseWorkbench : Screen("warehouse-workbench")
    data object WarehouseInventory : Screen("warehouse-inventory?filter={filter}") {
        fun createRoute(filter: String = "ALL") = "warehouse-inventory?filter=" + Uri.encode(filter)
    }
    data object WarehouseAnalysis : Screen("warehouse-analysis")
    data object WarehouseAnalysisProduct : Screen("warehouse-analysis/{productId}") {
        fun createRoute(id: String) = "warehouse-analysis/" + Uri.encode(id)
    }
    data object WarehouseProduct : Screen("warehouse-product/{productId}") {
        fun createRoute(id: String) = "warehouse-product/" + Uri.encode(id)
    }
    data object WarehouseReceive : Screen("warehouse-receive")
    data object WarehouseOutbound : Screen("warehouse-outbound")
    data object WarehouseCount : Screen("warehouse-count")
    data object WarehouseEntry : Screen("warehouse-entry/{direction}/{source}?product={product}&batch={batch}&draft={draft}") {
        fun createRoute(direction: String, source: String, product: String? = null, batch: String? = null, draft: String? = null): String =
            "warehouse-entry/" + direction + "/" + source + "?product=" + Uri.encode(product.orEmpty()) +
                "&batch=" + Uri.encode(batch.orEmpty()) + "&draft=" + Uri.encode(draft.orEmpty())
    }
    data object WarehousePending : Screen("warehouse-pending/{direction}") {
        fun createRoute(direction: String) = "warehouse-pending/" + direction
    }
    data object WarehouseTransfer : Screen("warehouse-transfer/{productId}") {
        fun createRoute(id: String) = "warehouse-transfer/" + Uri.encode(id)
    }
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object SearchCenter : Screen("search-center")
    data object AgentChat : Screen("agent-chat")
    data object Save : Screen("save?imageUri={imageUri}&imageUris={imageUris}&mode={mode}") {
        fun createRoute(
            imageUri: String? = null,
            imageUris: List<String> = emptyList(),
            mode: String = "append"
        ): String {
            val encodedUri = imageUri?.let(Uri::encode) ?: ""
            val encodedUris = imageUris.joinToString(",") { Uri.encode(it) }
            return "save?imageUri=$encodedUri&imageUris=$encodedUris&mode=$mode"
        }
    }

    data object Detail : Screen("detail/{itemId}/{scope}") {
        const val ScopeAll = "all"
        const val ScopeUsedUp = "used_up"
        const val ScopePendingReview = "pending_review"
        const val ScopeReviewed = "reviewed"

        fun createRoute(itemId: Long, scope: String = ScopeAll): String = "detail/$itemId/$scope"
    }

    data object Search : Screen("search")
    data object Category : Screen("category")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Trash : Screen("trash")
    data object Expiry : Screen("expiry")
    data object Edit : Screen("edit/{itemId}?imageUri={imageUri}&imageUris={imageUris}&mode={mode}") {
        fun createRoute(
            itemId: Long,
            imageUri: String? = null,
            imageUris: List<String> = emptyList(),
            mode: String = "append"
        ): String {
            val encodedUri = imageUri?.let(Uri::encode) ?: ""
            val encodedUris = imageUris.joinToString(",") { Uri.encode(it) }
            return "edit/$itemId?imageUri=$encodedUri&imageUris=$encodedUris&mode=$mode"
        }
    }
}
