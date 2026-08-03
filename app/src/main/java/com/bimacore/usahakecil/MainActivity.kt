package com.bimacore.usahakecil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.bimacore.usahakecil.ui.HomeScreen
import com.bimacore.usahakecil.ui.OperationsViewModel
import com.bimacore.usahakecil.ui.PosViewModel
import com.bimacore.usahakecil.ui.theme.UsahaKecilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val posApplication = application as PosApplication
        val guidePreferences = getSharedPreferences(
            FirstRunGuidePreferences.FILE_NAME,
            MODE_PRIVATE,
        )
        setContent {
            UsahaKecilTheme {
                var showFirstRunGuide by remember {
                    mutableStateOf(
                        !guidePreferences.getBoolean(
                            FirstRunGuidePreferences.COMPLETED_KEY,
                            false,
                        ),
                    )
                }
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
                    onRecreate = {
                        viewModelStore.clear()
                        recreate()
                    },
                    showFirstRunGuide = showFirstRunGuide,
                    onFirstRunGuideComplete = {
                        guidePreferences.edit()
                            .putBoolean(FirstRunGuidePreferences.COMPLETED_KEY, true)
                            .apply()
                        showFirstRunGuide = false
                    },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (application as PosApplication).reportSession.lock()
    }
}
