package com.example.videodownloader.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.PlatformDownloadRule

import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.res.stringResource
import com.example.videodownloader.R

@Composable
fun customSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
fun PlatformRowItem(
    name: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    rule: PlatformDownloadRule,
    isConflict: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (ruleText, ruleIcon) = when (rule) {
                    PlatformDownloadRule.VIDEO -> stringResource(R.string.rule_video_label) to Icons.Default.Videocam
                    PlatformDownloadRule.AUDIO -> stringResource(R.string.rule_audio_label) to Icons.Default.MusicNote
                    PlatformDownloadRule.PREVIEW_ONLY -> stringResource(R.string.rule_analysis_label) to (if (isConflict) Icons.Default.Visibility else Icons.Default.Search)
                }
                Icon(
                    ruleIcon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isConflict) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ruleText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isConflict) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SelectableRuleOption(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface, label = "optionBg")
    val borderColor by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "optionBorder")
    Surface(shape = RoundedCornerShape(14.dp), color = containerColor, border = BorderStroke(1.dp, borderColor), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onClick() }) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = { onClick() })
        }
    }
}

@Composable
fun AboutFeatureRow(icon: ImageVector, iconBg: Color, iconColor: Color, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SelectablePlayerRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
fun PlatformChip(name: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Text(text = name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
fun TechSpecCardItem(icon: ImageVector, iconBg: Color, iconColor: Color, label: String, value: String, badgeText: String, badgeBg: Color, badgeColor: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) { Text(text = badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
        }
    }
}

@Composable
fun AccountPlatformCard(title: String, subtitle: String, icon: ImageVector, color: Color, isLoggedIn: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)), border = BorderStroke(1.dp, if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.width(8.dp))
            if (isLoggedIn) { Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = stringResource(R.string.account_connected_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } } } else { Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text(text = stringResource(R.string.login_action), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) } }
        }
    }
}
