package com.bimacore.usahakecil.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bimacore.usahakecil.data.CatalogSnapshot
import com.bimacore.usahakecil.data.CheckoutRequest
import com.bimacore.usahakecil.data.PosRepository
import com.bimacore.usahakecil.data.SaleUnitOption
import com.bimacore.usahakecil.data.ToppingEntity
import com.bimacore.usahakecil.domain.AddToCartResult
import com.bimacore.usahakecil.domain.CartItem
import com.bimacore.usahakecil.domain.CheckoutResult
import com.bimacore.usahakecil.domain.MoneyMath
import com.bimacore.usahakecil.domain.PaymentMethod
import com.bimacore.usahakecil.domain.Product
import com.bimacore.usahakecil.domain.ProductVariant
import com.bimacore.usahakecil.domain.Receipt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PosScreen {
    CASHIER_HOME,
    CATALOG,
    CART,
    PAYMENT,
    RECEIPT,
}

class PosViewModel(
    private val repository: PosRepository,
) : ViewModel() {
    val supportsCulinaryCustomization = repository.supportsCulinaryCustomization
    val supportsCustomerReceivables = repository.supportsCustomerReceivables
    val customers = repository.customers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )
    val snapshot: StateFlow<CatalogSnapshot> = repository.snapshot.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CatalogSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), null),
    )
    val sales = repository.sales.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _screen = MutableStateFlow(PosScreen.CASHIER_HOME)
    val screen = _screen.asStateFlow()

    private val _search = MutableStateFlow("")
    val search = _search.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(1L)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _variantProduct = MutableStateFlow<Product?>(null)
    val variantProduct = _variantProduct.asStateFlow()

    private val _unitProduct = MutableStateFlow<Product?>(null)
    val unitProduct = _unitProduct.asStateFlow()
    private val _unitOptions = MutableStateFlow<List<SaleUnitOption>>(emptyList())
    val unitOptions = _unitOptions.asStateFlow()
    private var pendingVariantId: Long? = null

    private val _customizeItem = MutableStateFlow<CartItem?>(null)
    val customizeItem = _customizeItem.asStateFlow()
    private val _availableToppings = MutableStateFlow<List<ToppingEntity>>(emptyList())
    val availableToppings = _availableToppings.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CASH)
    val paymentMethod = _paymentMethod.asStateFlow()

    private val _cashInput = MutableStateFlow("")
    val cashInput = _cashInput.asStateFlow()

    private val _externalPaymentConfirmed = MutableStateFlow(false)
    val externalPaymentConfirmed = _externalPaymentConfirmed.asStateFlow()
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()

    private val _receipt = MutableStateFlow<Receipt?>(null)
    val receipt = _receipt.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
            repository.loadCurrentReceipt()?.let {
                _receipt.value = it
                _screen.value = PosScreen.RECEIPT
            }
        }
    }

    fun setSearch(value: String) {
        _search.value = value
    }

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
    }

    fun tapProduct(product: Product) {
        if (product.hasVariants) {
            _variantProduct.value = product
            return
        }
        chooseUnitOrAdd(product, null)
    }

    fun selectVariant(variant: ProductVariant) {
        val product = _variantProduct.value ?: return
        _variantProduct.value = null
        chooseUnitOrAdd(product, variant.id)
    }

    fun dismissVariantPicker() {
        _variantProduct.value = null
    }

    fun selectUnit(option: SaleUnitOption) {
        val product = _unitProduct.value ?: return
        _unitProduct.value = null
        _unitOptions.value = emptyList()
        val variantId = pendingVariantId
        pendingVariantId = null
        addProduct(product.id, variantId, option.id)
    }

    fun dismissUnitPicker() {
        _unitProduct.value = null
        _unitOptions.value = emptyList()
        pendingVariantId = null
    }

    fun customize(item: CartItem) {
        viewModelScope.launch {
            runCatching { repository.getAvailableToppings(item.productId) }
                .onSuccess {
                    _availableToppings.value = it
                    _customizeItem.value = item
                }
                .onFailure { showMessage(it.message ?: "Topping tidak dapat dibuka") }
        }
    }

    fun saveCustomization(
        note: String,
        toppingQuantities: Map<Long, Int>,
    ) {
        val item = _customizeItem.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.setCartCustomization(item.lineId, note, toppingQuantities)
            }.onSuccess {
                _customizeItem.value = null
                _availableToppings.value = emptyList()
            }.onFailure {
                showMessage(it.message ?: "Catatan pesanan gagal disimpan")
            }
        }
    }

    fun dismissCustomization() {
        _customizeItem.value = null
        _availableToppings.value = emptyList()
    }

    fun setQuantity(lineId: String, quantity: Int) {
        viewModelScope.launch {
            if (!repository.setQuantity(lineId, quantity)) {
                showMessage("Stok tidak cukup")
            }
        }
    }

    fun showCart() {
        _screen.value = PosScreen.CART
    }

    fun showCashierHome() {
        _screen.value = PosScreen.CASHIER_HOME
    }

    fun showCatalog() {
        _screen.value = PosScreen.CATALOG
    }

    fun showPayment() {
        if (snapshot.value.cartItems.isEmpty()) {
            showMessage("Keranjang masih kosong")
            return
        }
        _screen.value = PosScreen.PAYMENT
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
        _externalPaymentConfirmed.value = false
        if (method != PaymentMethod.CREDIT) _selectedCustomerId.value = null
    }

    fun setExternalPaymentConfirmed(value: Boolean) {
        _externalPaymentConfirmed.value = value
    }

    fun selectCustomer(id: Long?) {
        _selectedCustomerId.value = id
    }

    fun appendCashDigit(digit: Char) {
        if (!digit.isDigit()) return
        val candidate = (_cashInput.value + digit).trimStart('0').ifEmpty { "0" }
        val value = candidate.toLongOrNull() ?: return
        if (value <= MoneyMath.MAX_MONEY) _cashInput.value = candidate
    }

    fun deleteCashDigit() {
        _cashInput.value = _cashInput.value.dropLast(1)
    }

    fun setCashAmount(amount: Long) {
        if (amount in 0..MoneyMath.MAX_MONEY) _cashInput.value = amount.toString()
    }

    fun completeSale() {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val result = repository.completeSale(
                    CheckoutRequest(
                        method = _paymentMethod.value,
                        amountReceived = _cashInput.value.toLongOrNull() ?: 0L,
                        externalPaymentConfirmed = _externalPaymentConfirmed.value,
                        customerId = _selectedCustomerId.value,
                    ),
                )
                when (result) {
                    is CheckoutResult.Success -> {
                        _receipt.value = result.receipt
                        _screen.value = PosScreen.RECEIPT
                    }
                    is CheckoutResult.Error -> showMessage(result.message)
                }
            } catch (error: Exception) {
                showMessage(error.message ?: "Transaksi gagal disimpan")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun newTransaction() {
        viewModelScope.launch {
            repository.newTransaction()
            _receipt.value = null
            _cashInput.value = ""
            _externalPaymentConfirmed.value = false
            _paymentMethod.value = PaymentMethod.CASH
            _selectedCustomerId.value = null
            _screen.value = PosScreen.CATALOG
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun chooseUnitOrAdd(
        product: Product,
        variantId: Long?,
    ) {
        viewModelScope.launch {
            val units = repository.getSaleUnits(product.id)
            if (units.size > 1) {
                pendingVariantId = variantId
                _unitOptions.value = units
                _unitProduct.value = product
            } else {
                addProduct(product.id, variantId, units.firstOrNull()?.id)
            }
        }
    }

    private fun addProduct(
        productId: Long,
        variantId: Long?,
        unitId: Long?,
    ) {
        viewModelScope.launch {
            when (repository.addProduct(productId, variantId, unitId)) {
                AddToCartResult.Added -> Unit
                AddToCartResult.VariantRequired -> showMessage("Pilih varian produk dulu")
                AddToCartResult.OutOfStock -> showMessage("Stok produk habis atau tidak cukup")
                AddToCartResult.CompletedTransactionLocked ->
                    showMessage("Tekan Transaksi Baru dulu")
            }
        }
    }

    private fun showMessage(value: String) {
        _message.value = value
    }

    class Factory(
        private val repository: PosRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PosViewModel(repository) as T
    }
}
