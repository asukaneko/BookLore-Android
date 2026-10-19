package app.booklore.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.booklore.android.R

@Composable
internal fun LibraryNavigationBar(selectedTab: Int, localMode: Boolean, onSelected: (Int) -> Unit) {
    val labels = listOf(
        R.string.navigation_reading,
        if (localMode) R.string.local_library else R.string.navigation_libraries,
        R.string.navigation_series,
        R.string.settings,
    )
    val icons = listOf(
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.AutoMirrored.Filled.LibraryBooks,
        Icons.Filled.CollectionsBookmark,
        Icons.Filled.Settings,
    )
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            labels.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { onSelected(index) },
                    icon = { Icon(icons[index], contentDescription = null, modifier = Modifier.size(22.dp)) },
                    label = { Text(stringResource(label), fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun LibraryMark(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.size(64.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
internal fun LibrarySection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp), fontWeight = FontWeight.SemiBold)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
internal fun LibrarySettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = colorForEnabled(enabled))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorForEnabled(enabled))
            }
            Spacer(Modifier.width(12.dp))
            if (trailing != null) trailing() else if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(18.dp), tint = colorForEnabled(enabled))
            }
        }
    }
    if (onClick != null) Surface(onClick = onClick, enabled = enabled, modifier = modifier,
        color = Color.Transparent, shape = RoundedCornerShape(8.dp), content = content)
    else Surface(modifier = modifier, color = Color.Transparent, content = content)
}

@Composable
private fun colorForEnabled(enabled: Boolean): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.55f)
