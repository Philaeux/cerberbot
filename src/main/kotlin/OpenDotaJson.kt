data class OpenDotaRecentMatchesJSON(
    val match_id: Long,
    val duration: Int,
    val player_slot: Int
)

data class OpenDotaMatchJSON(
    val match_id: Long,
    val players: List<OpenDotaMatchPlayerJSON>
)

data class OpenDotaMatchPlayerJSON(
    val player_slot: Int,
    val account_id: Long?
)
