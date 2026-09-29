package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

private val DefaultMinLabelSize = 10.sp
private val DefaultMaxLabelSize = 14.sp
private val LabelStep = 0.5.sp

@Composable
fun ChatoneButtonText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
    color: Color = LocalContentColor.current,
    minSize: TextUnit = DefaultMinLabelSize,
    maxLines: Int = 1,
    textAlign: TextAlign = TextAlign.Center
) {
    val maxSize = style.fontSize.takeIf { it.isSpecified } ?: DefaultMaxLabelSize
    val floor = if (minSize.isSpecified && minSize.value < maxSize.value) minSize else maxSize

    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = floor,
            maxFontSize = maxSize,
            stepSize = LabelStep
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatoneActionRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
