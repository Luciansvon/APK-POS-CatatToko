package com.bimacore.usahakecil

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.bimacore.usahakecil.ui.HomeScreen
import com.bimacore.usahakecil.ui.BrandLoadingScreen
import com.bimacore.usahakecil.ui.OperationsViewModel
import com.bimacore.usahakecil.ui.PosViewModel
import com.bimacore.usahakecil.ui.theme.UsahaKecilTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var restoreInProgress = false

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
                val isInitializing by posViewModel.isInitializing.collectAsState()
                if (isInitializing) {
                    BrandLoadingScreen(getString(R.string.business_label))
                } else {
                    HomeScreen(
                        businessLabel = getString(R.string.business_label),
                        businessType = posApplication.businessType,
                        posViewModel = posViewModel,
                        operationsViewModel = operationsViewModel,
                        onRestore = restore@{ preview ->
                            if (restoreInProgress) return@restore
                            restoreInProgress = true
                            val backupManager = posApplication.newBackupManager()
                            viewModelStore.clear()
                            lifecycleScope.launch {
                                runCatching { backupManager.restore(preview) }
                                    .onFailure { error ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            error.message ?: "Pemulihan gagal",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                posApplication.reportSession.lock()
                                recreate()
                            }
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
    }

    override fun onStop() {
        super.onStop()
        (application as PosApplication).reportSession.lock()
    }
}
