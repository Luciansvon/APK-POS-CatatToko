package com.bimacore.usahakecil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimacore.usahakecil.ui.HomeScreen
import com.bimacore.usahakecil.ui.OperationsViewModel
import com.bimacore.usahakecil.ui.PosViewModel
import com.bimacore.usahakecil.ui.theme.UsahaKecilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val posApplication = application as PosApplication
        setContent {
            UsahaKecilTheme {
                val posViewModel: PosViewModel = viewModel(
                    factory = PosViewModel.Factory(posApplication.newPosRepository()),
                )
                val operationsViewModel: OperationsViewModel = viewModel(
                    factory = OperationsViewModel.Factory(posApplication),
                )
                HomeScreen(
                    businessLabel = getString(R.string.business_label),
                    businessType = posApplication.businessType,
                    posViewModel = posViewModel,
                    operationsViewModel = operationsViewModel,
                    onRecreate = ::recreate,
                )
            }
        }
    }
}
