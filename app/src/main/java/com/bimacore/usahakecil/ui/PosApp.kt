package com.bimacore.usahakecil.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.share.ReceiptImageExporter
import kotlinx.coroutines.launch

@Composable
fun PosApp(
    businessLabel: String,
    viewModel: PosViewModel,
    ownerUnlocked: Boolean,
    onOwnerAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val screen by viewModel.screen.collectAsState()
    val search by viewModel.search.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val variantProduct by viewModel.variantProduct.collectAsState()
    val unitProduct by viewModel.unitProduct.collectAsState()
    val unitOptions by viewModel.unitOptions.collectAsState()
    val customizeItem by viewModel.customizeItem.collectAsState()
    val availableToppings by viewModel.availableToppings.collectAsState()
    val paymentMethod by viewModel.paymentMethod.collectAsState()
    val cashInput by viewModel.cashInput.collectAsState()
    val externalConfirmed by viewModel.externalPaymentConfirmed.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()
    var showCalculator by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        snackbar.showSnackbar(value)
        viewModel.consumeMessage()
    }

    BackHandler(enabled = screen != PosScreen.CATALOG) {
        when (screen) {
            PosScreen.CART -> viewModel.showCatalog()
            PosScreen.PAYMENT -> viewModel.showCart()
            PosScreen.RECEIPT -> Unit
            PosScreen.CATALOG -> Unit
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val expanded = maxWidth >= 840.dp
        if (screen == PosScreen.RECEIPT && receipt != null) {
            ReceiptScreen(
                receipt = requireNotNull(receipt),
                isSharing = isSharing,
                onShare = {
                    if (isSharing) return@ReceiptScreen
                    isSharing = true
                    scope.launch {
                        try {
                            val shareIntent = ReceiptImageExporter.createShareIntent(
                                context = context,
                                receipt = requireNotNull(receipt),
                                primaryColor = primaryArgb,
                            )
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Bagikan struk"),
                            )
                        } catch (_: Exception) {
                            snackbar.showSnackbar("Struk PNG gagal dibuat")
                        } finally {
                            isSharing = false
                        }
                    }
                },
                onNewTransaction = viewModel::newTransaction,
            )
        } else if (expanded) {
            Row(Modifier.fillMaxSize()) {
                CatalogScreen(
                    businessLabel = businessLabel,
                    snapshot = snapshot,
                    search = search,
                    selectedCategoryId = selectedCategoryId,
                    compact = false,
                    onSearchChange = viewModel::setSearch,
                    onCategorySelected = viewModel::selectCategory,
                    onProductClick = viewModel::tapProduct,
                    onCartClick = viewModel::showCart,
                    onCalculatorClick = { showCalculator = true },
                    ownerUnlocked = ownerUnlocked,
                    onOwnerAccess = onOwnerAccess,
                    modifier = Modifier.weight(1.65f),
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                when (screen) {
                    PosScreen.PAYMENT -> PaymentScreen(
                        total = snapshot.cartItems.sumOf { it.subtotal },
                        method = paymentMethod,
                        cashInput = cashInput,
                        externalConfirmed = externalConfirmed,
                        allowCredit = viewModel.supportsCustomerReceivables,
                        customers = customers,
                        selectedCustomerId = selectedCustomerId,
                        isSaving = isSaving,
                        onBack = viewModel::showCart,
                        onMethodSelected = viewModel::setPaymentMethod,
                        onCashDigit = viewModel::appendCashDigit,
                        onCashDelete = viewModel::deleteCashDigit,
                        onCashAmount = viewModel::setCashAmount,
                        onExternalConfirmed = viewModel::setExternalPaymentConfirmed,
                        onCustomerSelected = viewModel::selectCustomer,
                        onComplete = viewModel::completeSale,
                        modifier = Modifier.weight(1f),
                    )
                    else -> CartScreen(
                        items = snapshot.cartItems,
                        compact = false,
                        onBack = viewModel::showCatalog,
                        onQuantityChange = viewModel::setQuantity,
                        onContinue = viewModel::showPayment,
                        onCustomize = if (viewModel.supportsCulinaryCustomization) {
                            viewModel::customize
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            when (screen) {
                PosScreen.CATALOG -> CatalogScreen(
                    businessLabel = businessLabel,
                    snapshot = snapshot,
                    search = search,
                    selectedCategoryId = selectedCategoryId,
                    compact = true,
                    onSearchChange = viewModel::setSearch,
                    onCategorySelected = viewModel::selectCategory,
                    onProductClick = viewModel::tapProduct,
                    onCartClick = viewModel::showCart,
                    onCalculatorClick = { showCalculator = true },
                    ownerUnlocked = ownerUnlocked,
                    onOwnerAccess = onOwnerAccess,
                )
                PosScreen.CART -> CartScreen(
                    items = snapshot.cartItems,
                    compact = true,
                    onBack = viewModel::showCatalog,
                    onQuantityChange = viewModel::setQuantity,
                    onContinue = viewModel::showPayment,
                    onCustomize = if (viewModel.supportsCulinaryCustomization) {
                        viewModel::customize
                    } else {
                        null
                    },
                )
                PosScreen.PAYMENT -> PaymentScreen(
                    total = snapshot.cartItems.sumOf { it.subtotal },
                    method = paymentMethod,
                    cashInput = cashInput,
                    externalConfirmed = externalConfirmed,
                    allowCredit = viewModel.supportsCustomerReceivables,
                    customers = customers,
                    selectedCustomerId = selectedCustomerId,
                    isSaving = isSaving,
                    onBack = viewModel::showCart,
                    onMethodSelected = viewModel::setPaymentMethod,
                    onCashDigit = viewModel::appendCashDigit,
                    onCashDelete = viewModel::deleteCashDigit,
                    onCashAmount = viewModel::setCashAmount,
                    onExternalConfirmed = viewModel::setExternalPaymentConfirmed,
                    onCustomerSelected = viewModel::selectCustomer,
                    onComplete = viewModel::completeSale,
                )
                PosScreen.RECEIPT -> Unit
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    variantProduct?.let { product ->
        VariantPicker(
            product = product,
            variants = snapshot.variants.filter { it.productId == product.id },
            onSelect = viewModel::selectVariant,
            onDismiss = viewModel::dismissVariantPicker,
        )
    }
    unitProduct?.let { product ->
        UnitPicker(
            product = product,
            units = unitOptions,
            onSelect = viewModel::selectUnit,
            onDismiss = viewModel::dismissUnitPicker,
        )
    }
    customizeItem?.let { item ->
        CulinaryCustomizationDialog(
            item = item,
            availableToppings = availableToppings,
            onDismiss = viewModel::dismissCustomization,
            onSave = viewModel::saveCustomization,
        )
    }
    if (showCalculator) {
        CalculatorDialog(onDismiss = { showCalculator = false })
    }
}
