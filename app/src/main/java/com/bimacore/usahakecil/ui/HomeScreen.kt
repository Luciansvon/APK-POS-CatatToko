package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.ui.theme.BrandColors

@Composable
fun HomeScreen(
    businessLabel: String,
    businessType: BusinessType,
    posViewModel: PosViewModel,
    operationsViewModel: OperationsViewModel,
    onRecreate: () -> Unit,
) {
    val ownerUnlocked by operationsViewModel.ownerUnlocked.collectAsState()
    val destinations = remember(operationsViewModel.capabilities, ownerUnlocked) {
        destinationsForAccess(operationsViewModel.capabilities, ownerUnlocked)
    }
    val presentation = remember(businessType) {
        navigationPresentationFor(businessType)
    }
    var destination by remember { mutableStateOf(AppDestination.POS) }
    var showOwnerAccess by remember { mutableStateOf(false) }
    val hasOwnerPin by operationsViewModel.reportHasPin.collectAsState()
    val message by operationsViewModel.message.collectAsState()
    val restoreCompleted by operationsViewModel.restoreCompleted.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val compactNavigation = LocalConfiguration.current.screenWidthDp < 600

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        snackbar.showSnackbar(value)
        operationsViewModel.consumeMessage()
    }
    LaunchedEffect(restoreCompleted) {
        if (restoreCompleted) onRecreate()
    }
    LaunchedEffect(ownerUnlocked) {
        if (!ownerUnlocked) destination = AppDestination.POS
    }

    Scaffold(
        bottomBar = {
            if (ownerUnlocked) {
                NavigationBar(
                    containerColor = BrandColors.NavigationBackground,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    destinations.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = BrandColors.NavigationIndicator,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppDestination.POS -> Icons.Outlined.PointOfSale
                                    AppDestination.OPERATIONS -> Icons.Outlined.Storefront
                                    AppDestination.FINANCE -> Icons.Outlined.Wallet
                                    AppDestination.REPORTS -> Icons.Outlined.Assessment
                                    AppDestination.MORE -> Icons.Outlined.MoreHoriz
                                },
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                when (item) {
                                    AppDestination.OPERATIONS -> presentation.operationsLabel
                                    AppDestination.FINANCE -> presentation.financeLabel
                                    else -> item.label
                                },
                                maxLines = 1,
                                softWrap = false,
                                style = if (compactNavigation && item == AppDestination.OPERATIONS) {
                                    LocalTextStyle.current.copy(fontSize = 10.sp)
                                } else {
                                    LocalTextStyle.current
                                },
                            )
                        },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (destination) {
                AppDestination.POS -> PosApp(
                    businessLabel = businessLabel,
                    viewModel = posViewModel,
                    ownerUnlocked = ownerUnlocked,
                    onOwnerAccess = { showOwnerAccess = true },
                )
                AppDestination.OPERATIONS -> OperationsScreen(
                    viewModel = operationsViewModel,
                    startSection = presentation.operationsStartSection,
                    title = presentation.operationsLabel,
                )
                AppDestination.FINANCE -> FinanceScreen(
                    viewModel = operationsViewModel,
                    startTab = presentation.financeStartTab,
                    title = presentation.financeLabel,
                )
                AppDestination.REPORTS -> ReportsScreen(operationsViewModel)
                AppDestination.MORE -> MoreScreen(
                    viewModel = operationsViewModel,
                    onExitOwner = operationsViewModel::lockReport,
                )
            }
        }
    }
    if (showOwnerAccess) {
        OwnerAccessDialog(
            hasPin = hasOwnerPin,
            ownerUnlocked = ownerUnlocked,
            onDismiss = { showOwnerAccess = false },
            onSubmit = { pin ->
                if (hasOwnerPin == false) operationsViewModel.createReportPin(pin)
                else operationsViewModel.unlockReport(pin)
                showOwnerAccess = false
            },
            onLock = {
                operationsViewModel.lockReport()
                showOwnerAccess = false
            },
        )
    }
}
