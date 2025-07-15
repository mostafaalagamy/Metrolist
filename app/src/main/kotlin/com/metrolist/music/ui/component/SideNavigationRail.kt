package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.utils.DeviceUtils

@Composable
fun SideNavigationRail(
    navigationItems: List<Screens>,
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    compactMode: Boolean = false
) {
    val railWidth = DeviceUtils.getNavigationRailWidth()
    
    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight(),
        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            navigationItems.forEach { screen ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                
                SideNavigationRailItem(
                    selected = isSelected,
                    screen = screen,
                    onClick = { onNavigate(screen.route) },
                    pureBlack = pureBlack,
                    compactMode = compactMode
                )
            }
        }
    }
}

@Composable
private fun SideNavigationRailItem(
    selected: Boolean,
    screen: Screens,
    onClick: () -> Unit,
    pureBlack: Boolean,
    compactMode: Boolean
) {
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        label = "icon_alpha"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            if (pureBlack) Color.White.copy(alpha = 0.1f) 
            else MaterialTheme.colorScheme.secondaryContainer
        } else Color.Transparent,
        label = "container_color"
    )
    
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Box(
                modifier = Modifier
                    .size(if (compactMode) 32.dp else 40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (selected) screen.iconIdActive else screen.iconIdInactive
                    ),
                    contentDescription = stringResource(screen.titleId),
                    modifier = Modifier
                        .size(24.dp)
                        .alpha(iconAlpha),
                    tint = if (selected) {
                        if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        label = if (!compactMode) {
            {
                Text(
                    text = stringResource(screen.titleId),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (selected) {
                        if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
                    } else {
                        if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        } else null,
        colors = NavigationRailItemDefaults.colors(
            selectedIconColor = Color.Transparent,
            unselectedIconColor = Color.Transparent,
            selectedTextColor = Color.Transparent,
            unselectedTextColor = Color.Transparent,
            indicatorColor = Color.Transparent
        )
    )
}