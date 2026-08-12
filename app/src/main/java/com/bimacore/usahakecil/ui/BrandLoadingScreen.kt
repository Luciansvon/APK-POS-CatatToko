package com.bimacore.usahakecil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.R

@Composable
fun BrandLoadingScreen(
    businessLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_loading_logo),
            contentDescription = "$businessLabel sedang dibuka",
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .widthIn(max = 560.dp)
                .heightIn(max = 220.dp)
                .testTag("brand-loading-logo"),
            contentScale = ContentScale.Fit,
        )
    }
}
