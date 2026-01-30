package com.example.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.finance.ui.theme.GlassBorder
import com.example.finance.ui.theme.GlassSurface

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassSurface.copy(alpha = 0.85f)) // Increased opacity for readability
            .border(
                BorderStroke(1.dp, GlassBorder),
                RoundedCornerShape(cornerRadius)
            )
            .padding(20.dp)
    ) {
        // Note: Real-time blur in Compose often requires RenderEffect (Android 12+) 
        // or a library like Cloudry's blurry. For standard Compose without external libs
        // or high minSdk, we simulate it with semi-transparent surface.
        // If targeting API 31+, we could use modifier.blur(20.dp) on the background
        content()
    }
}
