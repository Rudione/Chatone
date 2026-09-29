package io.rudione.chatone.domain.stream

import kotlin.math.abs

object StreamQualityResolver {

    fun selectionFor(preference: String, variants: List<StreamVariant>): StreamQualitySelection {
        val video = variants.filterNot { it.isAudioOnly }
        return when (preference) {
            StreamPlayerPreferences.QUALITY_AUTO -> StreamQualitySelection.Auto
            StreamPlayerPreferences.QUALITY_AUDIO_ONLY ->
                if (variants.any { it.isAudioOnly }) StreamQualitySelection.AudioOnly else StreamQualitySelection.Auto
            StreamVariant.SOURCE_GROUP ->
                video.firstOrNull { it.isSource }?.let { StreamQualitySelection.Fixed(it.groupId) }
                    ?: StreamQualitySelection.Auto
            else -> video.firstOrNull { it.groupId == preference }?.let { StreamQualitySelection.Fixed(it.groupId) }
                ?: closestBelow(preference, video)
        }
    }

    fun preferenceOf(selection: StreamQualitySelection): String = when (selection) {
        StreamQualitySelection.Auto -> StreamPlayerPreferences.QUALITY_AUTO
        StreamQualitySelection.AudioOnly -> StreamPlayerPreferences.QUALITY_AUDIO_ONLY
        is StreamQualitySelection.Fixed -> selection.groupId
    }

    fun variantFor(selection: StreamQualitySelection, variants: List<StreamVariant>): StreamVariant? = when (selection) {
        StreamQualitySelection.Auto -> null
        StreamQualitySelection.AudioOnly -> variants.firstOrNull { it.isAudioOnly }
        is StreamQualitySelection.Fixed -> variants.firstOrNull { it.groupId == selection.groupId }
    }

    fun activeVariant(bandwidth: Long?, height: Int?, variants: List<StreamVariant>): StreamVariant? {
        val video = variants.filterNot { it.isAudioOnly }
        if (video.isEmpty()) return null
        val byHeight = if (height != null && height > 0) video.filter { it.height == height } else video
        val candidates = byHeight.ifEmpty { video }
        if (bandwidth == null || bandwidth <= 0) return candidates.singleOrNull()
        return candidates.minByOrNull { abs(it.bandwidth - bandwidth) }
    }

    fun orderedForMenu(variants: List<StreamVariant>): List<StreamVariant> {
        val video = variants.filterNot { it.isAudioOnly }
            .sortedWith(compareByDescending<StreamVariant> { it.isSource }
                .thenByDescending { it.height }
                .thenByDescending { it.frameRate }
                .thenByDescending { it.bandwidth })
            .distinctBy { it.displayName }
        return video + variants.filter { it.isAudioOnly }.take(1)
    }

    private fun closestBelow(preference: String, video: List<StreamVariant>): StreamQualitySelection {
        val wantedHeight = preference.substringBefore('p').toIntOrNull()
            ?: return StreamQualitySelection.Auto
        val wantedFps = preference.substringAfter('p', "").toFloatOrNull() ?: 0f
        val match = video
            .filter { it.height in 1..wantedHeight }
            .maxWithOrNull(compareBy<StreamVariant> { it.height }.thenBy { -abs(it.frameRate - wantedFps) })
            ?: video.filter { it.height > 0 }.minByOrNull { it.height }
        return match?.let { StreamQualitySelection.Fixed(it.groupId) } ?: StreamQualitySelection.Auto
    }
}
