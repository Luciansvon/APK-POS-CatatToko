package com.bimacore.usahakecil.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimacore.usahakecil.backup.BackupPreview
import com.bimacore.usahakecil.domain.BusinessType

@Composable
fun HomeScreen(
    businessLabel: String,
    businessType: BusinessType,
    posViewModel: PosViewModel,
    operationsViewModel: OperationsViewModel,
    onRestore: (BackupPreview) -> Unit,
    showFirstRunGuide: Boolean,
    onFirstRunGuideComplete: () -> Unit,
) {
    val ownerUnlocked by operationsViewModel.ownerUnlocked.collectAsState()
    val destinations = remember(operationsViewModel.capabilities, ownerUnlocked) {
        destinationsForAccess(operationsViewModel.capabilities, ownerUnlocked)
    }
    val presentation = remember(businessType) {
        navigationPresentationFor(businessType)
    }
    var destination by remember {
        mutableStateOf(if (ownerUnlocked) AppDestination.REPORTS else AppDestination.POS)
    }
    var showOperationsOverview by remember { mutableStateOf(false) }
    var operationsStartSection by remember { mutableStateOf(presentation.operationsStartSection) }
    var financeStartTab by remember { mutableStateOf(presentation.financeStartTab) }
    var showOwnerAccess by remember { mutableStateOf(false) }
    var showShiftOpen by remember { mutableStateOf(false) }
    var showHistoryImport by remember { mutableStateOf(false) }
    val hasOwnerPin by operationsViewModel.reportHasPin.collectAsState()
    val hasOpenShift by operationsViewModel.hasOpenShift.collectAsState()
    val message by operationsViewModel.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val compactNavigation = LocalConfiguration.current.screenWidthDp < 600

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        snackbar.showSnackbar(value)
        operationsViewModel.consumeMessage()
    }
    LaunchedEffect(ownerUnlocked) {
        destination = if (ownerUnlocked) AppDestination.REPORTS else AppDestination.POS
        showOperationsOverview = false
    }

    if (showHistoryImport && ownerUnlocked) {
        HistoryImportScreen(
            viewModel = operationsViewModel,
            onBack = { showHistoryImport = false },
        )
        return
    }

    Scaffold(
        bottomBar = {
            if (ownerUnlocked) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    destinations.forEach { item ->
                        val isSelected = destination == item
                        val selectedIndicatorColor = MaterialTheme.colorScheme.primary
                        NavigationBarItem(
                            modifier = Modifier.drawWithContent {
                                drawContent()
                                if (isSelected && item == AppDestination.OPERATIONS) {
                                    drawRect(
                                        color = selectedIndicatorColor,
                                        size = androidx.compose.ui.geometry.Size(
                                            width = size.width,
                                            height = 2.dp.toPx(),
                                        ),
                                    )
                                }
                            },
                            selected = isSelected,
                            onClick = {
                                if (item == AppDestination.OPERATIONS) {
                                    operationsStartSection = presentation.operationsStartSection
                                    showOperationsOverview = true
                                } else if (item == AppDestination.FINANCE) {
                                    financeStartTab = presentation.financeStartTab
                                    showOperationsOverview = false
                                } else {
                                    showOperationsOverview = false
                                }
                                destination = item
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    AppDestination.POS -> Icons.Outlined.PointOfSale
                                    AppDestination.OPERATIONS -> Icons.AutoMirrored.Outlined.Assignment
                                    AppDestination.FINANCE -> Icons.Outlined.Wallet
                                    AppDestination.REPORTS -> Icons.Outlined.Description
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
                    hasOpenShift = hasOpenShift,
                    ownerUnlocked = ownerUnlocked,
                    onOwnerAccess = { showOwnerAccess = true },
                    onOpenShift = { showShiftOpen = true },
                    onStartTransaction = {
                        if (!ownerUnlocked && !hasOpenShift) showShiftOpen = true
                        else posViewModel.showCatalog()
                    },
                    onRegisterUnknownBarcode = { barcode ->
                        operationsViewModel.prefillBarcodeForManagement(barcode)
                        operationsStartSection = "Barcode"
                        showOperationsOverview = false
                        destination = AppDestination.OPERATIONS
                    },
                )
                AppDestination.OPERATIONS -> if (showOperationsOverview) {
                    OwnerOperationsOverview(
                        viewModel = operationsViewModel,
                        onOpenOperations = { section ->
                            operationsStartSection = section
                            showOperationsOverview = false
                        },
                        onOpenFinance = {
                            showOperationsOverview = false
                            financeStartTab = 0
                            destination = AppDestination.FINANCE
                        },
                        onOpenReports = {
                            showOperationsOverview = false
                            destination = AppDestination.REPORTS
                        },
                        onOpenMore = {
                            showOperationsOverview = false
                            destination = AppDestination.MORE
                        },
                    )
                } else {
                    OperationsScreen(
                        viewModel = operationsViewModel,
                        startSection = operationsStartSection,
                        title = presentation.operationsLabel,
                    )
                }
                AppDestination.FINANCE -> FinanceScreen(
                    viewModel = operationsViewModel,
                    startTab = financeStartTab,
                    title = presentation.financeLabel,
                )
                AppDestination.REPORTS -> ReportsScreen(operationsViewModel)
                AppDestination.MORE -> MoreScreen(
                    viewModel = operationsViewModel,
                    onExitOwner = operationsViewModel::lockReport,
                    onOpenHistoryImport = { showHistoryImport = true },
                    onRestore = onRestore,
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
    if (showShiftOpen) {
        ShiftOpenDialog(
            onDismiss = { showShiftOpen = false },
        ) { cashierName, openingCash, note ->
            operationsViewModel.openShift(cashierName, openingCash, note)
            showShiftOpen = false
        }
    }
    if (showFirstRunGuide) {
        com.bimacore.usahakecil.FirstRunGuide(
            businessLabel = businessLabel,
            onComplete = onFirstRunGuideComplete,
        )
    }
}
