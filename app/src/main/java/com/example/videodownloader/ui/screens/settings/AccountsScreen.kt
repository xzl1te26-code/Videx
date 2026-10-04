package com.example.videodownloader.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.videodownloader.AuthManager

import androidx.compose.ui.res.stringResource
import com.example.videodownloader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenLogin: (String, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var cookieStates by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var targetLogoutPlatform by remember { mutableStateOf<String?>(null) }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                cookieStates = AuthManager.checkLoginStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.logout_all_dialog_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text(stringResource(R.string.logout_all_dialog_desc), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            AuthManager.clearAllCookies()
                            cookieStates = AuthManager.checkLoginStates()
                            showLogoutDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.all_sessions_cleared_toast)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.logout_action), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showLogoutDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel_action))
                    }
                }
            },
            dismissButton = {}
        )
    }
    
    if (targetLogoutPlatform != null) {
        val platformName = when (targetLogoutPlatform) { "ig" -> "Instagram"; "vk" -> "VKontakte"; "yt" -> "YouTube"; "tt" -> "TikTok"; else -> "" }
        val switchAccountUrl = when (targetLogoutPlatform) {
            "ig" -> "https://www.instagram.com/accounts/login/?force_authentication=1"
            "vk" -> "https://id.vk.com/auth"
            "yt" -> "https://accounts.google.com/Logout?continue=https%3A%2F%2Faccounts.google.com%2FServiceLogin%3Fservice%3Dyoutube%26continue%3Dhttps%253A%252F%252Fm.youtube.com%252F%26prompt%3Dselect_account"
            "tt" -> "https://www.tiktok.com/login"
            else -> ""
        }
        val siteUrl = when (targetLogoutPlatform) {
            "ig" -> "https://www.instagram.com"
            "vk" -> "https://m.vk.com"
            "yt" -> "https://m.youtube.com"
            "tt" -> "https://www.tiktok.com"
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { targetLogoutPlatform = null },
            icon = { Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp)) } },
            title = { Text(text = stringResource(R.string.account_platform_title, platformName), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = { Text(text = stringResource(R.string.account_authorized_desc), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { val platform = targetLogoutPlatform!!; AuthManager.clearCookiesForPlatform(platform); cookieStates = AuthManager.checkLoginStates(); targetLogoutPlatform = null; onOpenLogin(switchAccountUrl, context.getString(R.string.login_to_platform, platformName)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.change_account_action), fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(onClick = { onOpenLogin(siteUrl, platformName); targetLogoutPlatform = null }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.check_profile_action))
                    }
                    FilledTonalButton(
                        onClick = {
                            AuthManager.clearCookiesForPlatform(targetLogoutPlatform!!)
                            cookieStates = AuthManager.checkLoginStates()
                            val msg = context.getString(R.string.logged_out_toast, platformName)
                            targetLogoutPlatform = null
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.logout_account_action), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { targetLogoutPlatform = null }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel_action), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            dismissButton = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description)) } },
                actions = { IconButton(onClick = { showLogoutDialog = true }) { Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.logout_all_content_description), tint = MaterialTheme.colorScheme.error) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = stringResource(R.string.accounts_info_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
                        }
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Text(text = stringResource(R.string.accounts_security_info), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                    }
                }
            }
            item { AccountPlatformCard(title = "Instagram", subtitle = stringResource(R.string.instagram_subtitle), icon = Icons.Default.CameraAlt, color = Color(0xFFE1306C), isLoggedIn = cookieStates["ig"] == true, onClick = { if (cookieStates["ig"] == true) targetLogoutPlatform = "ig" else onOpenLogin("https://www.instagram.com/accounts/login/?force_authentication=1", context.getString(R.string.login_to_instagram)) }) }
            item { AccountPlatformCard(title = "VKontakte", subtitle = stringResource(R.string.vk_subtitle), icon = Icons.Default.SlowMotionVideo, color = Color(0xFF0077FF), isLoggedIn = cookieStates["vk"] == true, onClick = { if (cookieStates["vk"] == true) targetLogoutPlatform = "vk" else onOpenLogin("https://m.vk.com/login", context.getString(R.string.login_to_vk)) }) }
            item { AccountPlatformCard(title = "YouTube", subtitle = stringResource(R.string.youtube_subtitle), icon = Icons.Default.PlayCircle, color = Color(0xFFFF0000), isLoggedIn = cookieStates["yt"] == true, onClick = { if (cookieStates["yt"] == true) targetLogoutPlatform = "yt" else onOpenLogin("https://accounts.google.com/Logout?continue=https%3A%2F%2Faccounts.google.com%2FServiceLogin%3Fservice%3Dyoutube%26continue%3Dhttps%253A%252F%252Fm.youtube.com%252F%26prompt%3Dselect_account", context.getString(R.string.login_to_youtube)) }) }
            item { AccountPlatformCard(title = "TikTok", subtitle = stringResource(R.string.tiktok_subtitle), icon = Icons.Default.MusicVideo, color = Color(0xFF000000), isLoggedIn = cookieStates["tt"] == true, onClick = { if (cookieStates["tt"] == true) targetLogoutPlatform = "tt" else onOpenLogin("https://www.tiktok.com/login", context.getString(R.string.login_to_tiktok)) }) }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
