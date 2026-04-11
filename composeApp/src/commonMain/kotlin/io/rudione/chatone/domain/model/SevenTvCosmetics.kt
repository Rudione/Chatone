package io.rudione.chatone.domain.model


object SevenTvCosmetics {

    data class Paint(
        val id: String,
        val name: String,
        val function: String,
        val color: Int?,
        val stops: List<PaintStop>,
        val repeat: Boolean,
        val angle: Int,
        val imageUrl: String,
        val shadows: List<PaintShadow>
    )

    data class PaintStop(
        val at: Float,
        val color: Int
    )

    data class PaintShadow(
        val xOffset: Float,
        val yOffset: Float,
        val radius: Float,
        val color: Int
    )

    data class Badge(
        val id: String,
        val name: String,
        val tooltip: String,
        val url1x: String,
        val url2x: String,
        val url3x: String
    )
}


data class SevenTvUserCosmetic(
    val sevenTvId: String,
    val paint: SevenTvCosmetics.Paint? = null,
    val badge: SevenTvCosmetics.Badge? = null,
    val nameColor: Int? = null
)
