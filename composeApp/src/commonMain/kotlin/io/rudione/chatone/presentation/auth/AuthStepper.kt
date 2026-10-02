package io.rudione.chatone.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.CircleUser
import io.rudione.chatone.icons.lucide.ClipboardPaste
import io.rudione.chatone.icons.lucide.Globe
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

private enum class StepMark { Done, Active, Upcoming }

fun AuthStep.icon(automatic: Boolean): ImageVector = when (this) {
    AuthStep.SIGN_IN -> Lucide.CircleUser
    AuthStep.PASTE -> if (automatic) Lucide.Globe else Lucide.ClipboardPaste
    AuthStep.RIGHTS -> Lucide.ShieldCheck
}

fun AppStrings.stepLabel(step: AuthStep, automatic: Boolean): String = when (step) {
    AuthStep.SIGN_IN -> authStepSignIn
    AuthStep.PASTE -> if (automatic) authStepBrowser else authStepPaste
    AuthStep.RIGHTS -> authStepRights
}

@Composable
fun AuthStepper(slots: List<AuthStep>, current: AuthStep, automatic: Boolean, modifier: Modifier = Modifier) {
    val position = slots.indexOf(current)
    Row(
        modifier = modifier
            .expressiveGlass(CircleShape, elevation = 10.dp)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.forEachIndexed { index, step ->
            val mark = when {
                index < position -> StepMark.Done
                index == position -> StepMark.Active
                else -> StepMark.Upcoming
            }
            StepperItem(step = step, mark = mark, automatic = automatic)
        }
    }
}

@Composable
private fun StepperItem(step: AuthStep, mark: StepMark, automatic: Boolean) {
    val s = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (mark) {
            StepMark.Active -> scheme.primary
            StepMark.Done -> scheme.primary.copy(alpha = 0.16f)
            StepMark.Upcoming -> Color.Transparent
        },
        tween(260),
        label = "stepContainer"
    )
    val content by animateColorAsState(
        when (mark) {
            StepMark.Active -> scheme.onPrimary
            StepMark.Done -> scheme.primary
            StepMark.Upcoming -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        tween(260),
        label = "stepContent"
    )
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = mark == StepMark.Done,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.45f), initialScale = 0.3f) + fadeIn()) togetherWith
                    (scaleOut(targetScale = 0.3f) + fadeOut())
            },
            label = "stepIcon"
        ) { done ->
            Icon(
                if (done) Lucide.Check else step.icon(automatic),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(
            visible = mark == StepMark.Active,
            enter = expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200, 80)),
            exit = shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium)) + fadeOut(tween(120))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    s.stepLabel(step, automatic),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}
