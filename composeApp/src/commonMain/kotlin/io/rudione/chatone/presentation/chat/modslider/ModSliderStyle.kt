package io.rudione.chatone.presentation.chat.modslider

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.moderation.ModSliderDurations
import io.rudione.chatone.domain.moderation.ModSliderAction
import io.rudione.chatone.domain.moderation.ModSliderMetrics
import io.rudione.chatone.domain.moderation.ModSliderTrack
import io.rudione.chatone.icons.lucide.Ban
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.ShieldCheck
import io.rudione.chatone.icons.lucide.Timer
import io.rudione.chatone.icons.lucide.Trash2
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.ModSliderStrings
import androidx.compose.ui.unit.Density

@Stable
class ModSliderStyle(
    val delete: Color,
    val timeout: Color,
    val ban: Color,
    val unban: Color,
    val neutral: Color,
    val card: Color,
    val onTrack: Color,
    val measurer: TextMeasurer,
    val labelStyle: TextStyle,
    private val deleteIcon: VectorPainter,
    private val timeoutIcon: VectorPainter,
    private val banIcon: VectorPainter,
    private val unbanIcon: VectorPainter,
    private val strings: ModSliderStrings
) {
    val gripLabel: String get() = strings.grip

    fun colorOf(action: ModSliderAction?): Color = when (action) {
        ModSliderAction.Delete -> delete
        is ModSliderAction.Timeout -> timeout
        ModSliderAction.Ban -> ban
        ModSliderAction.Unban -> unban
        null -> neutral
    }

    fun iconOf(action: ModSliderAction): VectorPainter = when (action) {
        ModSliderAction.Delete -> deleteIcon
        is ModSliderAction.Timeout -> timeoutIcon
        ModSliderAction.Ban -> banIcon
        ModSliderAction.Unban -> unbanIcon
    }

    val chipReferenceLabel: String by lazy { labelOf(ModSliderAction.Timeout(REFERENCE_SECONDS)) }

    fun chipLabelOf(action: ModSliderAction): String = if (action == ModSliderAction.Delete) "" else labelOf(action)

    fun labelOf(action: ModSliderAction): String = when (action) {
        ModSliderAction.Delete -> strings.actionDelete
        is ModSliderAction.Timeout -> strings.actionTimeout.replace("{0}", ModSliderDurations.format(action.seconds))
        ModSliderAction.Ban -> strings.actionBan
        ModSliderAction.Unban -> strings.actionUnban
    }
}

@Stable
class ModSliderBinding(
    val host: ModSliderHost,
    val style: ModSliderStyle,
    val durations: List<Int>,
    val deleteAtStart: Boolean,
    val banAtEnd: Boolean,
    val travelFraction: Float
) {
    fun trackFor(canDelete: Boolean, canPunish: Boolean): ModSliderTrack = ModSliderTrack.build(
        deleteAtStart = deleteAtStart,
        durations = durations,
        banAtEnd = banAtEnd,
        canDelete = canDelete,
        canPunish = canPunish
    )
}

class ModSliderHandle(
    val state: ModSliderState,
    val track: ModSliderTrack,
    val binding: ModSliderBinding,
    val hovered: () -> Boolean,
    val onAction: (ModSliderAction) -> Unit
) {
    internal var timestampEnd by mutableFloatStateOf(0f)
        private set

    internal var zoneHovered by mutableStateOf(false)

    fun reportTimestamp(layout: TextLayoutResult, length: Int) {
        timestampEnd = if (length > 0 && layout.lineCount > 0 && layout.layoutInput.text.length >= length) {
            layout.getHorizontalPosition(length, usePrimaryDirection = true)
        } else 0f
    }
}

internal fun Density.modSliderMetrics(): ModSliderMetrics = ModSliderMetrics(
    deadZone = 14.dp.toPx(),
    minTravel = 160.dp.toPx(),
    backwardReach = 64.dp.toPx(),
    hysteresis = 4.dp.toPx(),
    overshoot = 36.dp.toPx()
)

@Composable
fun rememberModSliderStyle(): ModSliderStyle {
    val extra = ChatoneTheme.extraColors
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current.modSlider
    val measurer = rememberTextMeasurer(cacheSize = LABEL_CACHE_SIZE)
    val deleteIcon = rememberVectorPainter(Lucide.Trash2)
    val timeoutIcon = rememberVectorPainter(Lucide.Timer)
    val banIcon = rememberVectorPainter(Lucide.Ban)
    val unbanIcon = rememberVectorPainter(Lucide.ShieldCheck)
    return remember(extra, scheme, strings, measurer, deleteIcon, timeoutIcon, banIcon, unbanIcon) {
        ModSliderStyle(
            delete = extra.modDelete,
            timeout = extra.modTimeout,
            ban = extra.modBan,
            unban = extra.modUnban,
            neutral = scheme.onSurfaceVariant,
            card = scheme.surfaceContainerHigh,
            onTrack = scheme.onSurface,
            measurer = measurer,
            labelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            deleteIcon = deleteIcon,
            timeoutIcon = timeoutIcon,
            banIcon = banIcon,
            unbanIcon = unbanIcon,
            strings = strings
        )
    }
}

private const val LABEL_CACHE_SIZE = 24
private const val REFERENCE_SECONDS = 59 * 60

@Composable
fun rememberModSliderHandle(
    binding: ModSliderBinding,
    canDelete: Boolean,
    canPunish: Boolean,
    hovered: () -> Boolean,
    onAction: (ModSliderAction) -> Unit
): ModSliderHandle? {
    val state = rememberModSliderState(binding.host)
    val track = remember(binding, canDelete, canPunish) { binding.trackFor(canDelete, canPunish) }
    val latestAction = rememberUpdatedState(onAction)
    val latestHover = rememberUpdatedState(hovered)
    val handle = remember(state, track, binding) {
        ModSliderHandle(state, track, binding, hovered = { latestHover.value() }) { action -> latestAction.value(action) }
    }
    return handle.takeUnless { track.isEmpty }
}
