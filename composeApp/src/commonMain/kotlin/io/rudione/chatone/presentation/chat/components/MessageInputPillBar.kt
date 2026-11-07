package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.animatedBorder
import io.rudione.chatone.presentation.theme.panelBlur

private val PlainPillShape = RoundedCornerShape(24.dp)

@Composable
internal fun MessageInputPillBar(
    glass: ChatInputGlass?,
    metrics: MessageInputMetrics,
    surface: InputSurface,
    bottomBarColor: Color,
    bottomBarBlur: Float,
    showSend: Boolean,
    pill: @Composable BoxScope.() -> Unit,
    send: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (glass == null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(bottomBarColor)
                    .then(if (bottomBarBlur > 0f) Modifier.panelBlur(bottomBarBlur) else Modifier)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (glass == null) {
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    } else {
                        Modifier.padding(
                            start = glass.style.horizontalMargin,
                            end = glass.style.horizontalMargin,
                            top = glass.style.topMargin,
                            bottom = glass.style.bottomMargin
                        )
                    }
                ),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (glass == null) {
                            Modifier
                                .shadow(
                                    elevation = if (surface.focused) 4.dp else 1.dp,
                                    shape = PlainPillShape,
                                    ambientColor = surface.shadow,
                                    spotColor = surface.shadow
                                )
                                .animatedBorder(surface.borderWidth, PlainPillShape, surface.border)
                                .clip(PlainPillShape)
                                .drawBehind { drawRect(surface.resting()) }
                        } else {
                            Modifier.chatInputGlassSurface(
                                glass = glass,
                                resting = surface.resting,
                                border = surface.border,
                                borderWidth = surface.borderWidth,
                                focused = surface.focused,
                                glow = surface.shadow
                            )
                        }
                    ),
                content = pill
            )
            if (metrics.touch) {
                AnimatedVisibility(
                    visible = showSend,
                    enter = expandHorizontally(expandFrom = Alignment.Start, clip = false) +
                        scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), 0.3f) +
                        fadeIn(tween(120)),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false) +
                        scaleOut(tween(160), 0.3f) +
                        fadeOut(tween(120))
                ) {
                    Box(modifier = Modifier.padding(start = 6.dp)) { send() }
                }
            }
        }
    }
}
