package com.bimacore.usahakecil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimacore.usahakecil.R

/** Shared Owner-only visual tokens. Cashier controls keep the shared/default style. */
internal val OwnerActionShape = RoundedCornerShape(12.dp)
internal val OwnerCardShape = RoundedCornerShape(12.dp)
internal val OwnerPageHorizontalPadding = 16.dp
internal val OwnerGroupSpacing = 12.dp

/**
 * Shared in-app rendering of the flavor-specific adaptive launcher icon.
 *
 * The launcher XML is adaptive and cannot be used as a regular Compose painter on
 * every supported API level, so this keeps the same foreground/background pair
 * explicit while remaining shared across Cashier and Owner surfaces.
 */
@Composable
internal fun CatatTokoAppIcon(
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val taggedModifier = testTag?.let { Modifier.testTag(it) } ?: Modifier
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(colorResource(R.color.ic_launcher_background))
            .then(taggedModifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground_v3),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun OwnerBentoSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, OwnerCardShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = OwnerCardShape,
            ),
    ) {
        content()
    }
}

/** Compact shared brand header for every Owner/non-cashier page. */
@Composable
internal fun CatatTokoOwnerHeader(
    pageTitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    onOwnerAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "catattoko-owner-header",
    ownerActionTestTag: String = "catattoko-owner-action",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = OwnerPageHorizontalPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            navigationIcon?.invoke()
            CatatTokoAppIcon(
                size = 40.dp,
                contentDescription = stringResource(R.string.brand_logo_description),
            )
            Column(modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.brand_name),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("catattoko-owner-brand"),
                )
                androidx.compose.material3.Text(
                    text = pageTitle ?: "Mode Owner",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onOwnerAction != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOwnerAction)
                        .testTag(ownerActionTestTag),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = "Buka pengaturan Owner",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}
