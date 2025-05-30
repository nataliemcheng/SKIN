package com.example.skn.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class NavigationTab {
    HOME,
    SEARCH,
    SCAN,
    PROFILE,
}

@Composable
fun AppBottomNavigation(
    selectedTab: NavigationTab,
    onHomeClick: () -> Unit,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor   = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor      = MaterialTheme.colorScheme.secondaryContainer
    )
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            selected = selectedTab == NavigationTab.HOME,
            onClick = onHomeClick,
            colors   = navItemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            selected = selectedTab == NavigationTab.SEARCH,
            onClick = onSearchClick,
            colors   = navItemColors

        )
        // Camera/Scan (center icon)
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Scan Product",
                    modifier = Modifier.size(30.dp)
                )
            },
            selected = selectedTab == NavigationTab.SCAN,
            onClick = onScanClick,
            colors   = navItemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
            selected = selectedTab == NavigationTab.PROFILE,
            onClick = onProfileClick,
            colors   = navItemColors
        )

    }
}