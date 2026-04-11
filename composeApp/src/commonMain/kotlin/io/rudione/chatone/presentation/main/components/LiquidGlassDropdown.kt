package io.rudione.chatone.presentation.main.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.LiquidGlassSurface

@Composable
fun LiquidGlassDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
       
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                       
                        onPress = { onDismissRequest() }
                    )
                }
        ) {
           
            LiquidGlassSurface(
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                backgroundAlphaHigh = 0.94f,
                backgroundAlphaLow = 0.85f,
                modifier = modifier
                    .widthIn(min = 160.dp, max = 240.dp)
                   
                   
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { /* consume tap */ })
                    }
            ) {
                Column(content = content)
            }
        }
    }
}