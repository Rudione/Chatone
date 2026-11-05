package io.rudione.chatone.presentation.components

import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

private val FitStep = 0.5.sp
private val DefaultMinFitSize = 10.sp
private const val MARQUEE_DELAY_MS = 1_400

@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = 1,
    minFontSize: TextUnit = DefaultMinFitSize
) {
    val resolved = style.merge(
        color = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } },
        fontWeight = fontWeight,
        textAlign = textAlign
    )
    val maxSize = resolved.fontSize.takeIf { it.isSpecified } ?: 14.sp
    val floor = if (minFontSize.isSpecified && minFontSize.value < maxSize.value) minFontSize else maxSize
    var scrolls by remember(text, maxLines) { mutableStateOf(false) }
    if (scrolls) {
        BasicText(
            text = text,
            modifier = modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                animationMode = MarqueeAnimationMode.Immediately,
                repeatDelayMillis = MARQUEE_DELAY_MS,
                initialDelayMillis = MARQUEE_DELAY_MS,
                velocity = 28.dp
            ),
            style = resolved.copy(fontSize = floor),
            maxLines = 1,
            softWrap = false
        )
    } else {
        BasicText(
            text = text,
            modifier = modifier,
            style = resolved,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = floor, maxFontSize = maxSize, stepSize = FitStep),
            onTextLayout = { layout -> if (maxLines == 1 && layout.hasVisualOverflow) scrolls = true }
        )
    }
}
