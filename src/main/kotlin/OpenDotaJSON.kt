/**
 * Result returned by a OpenDota endpoint, giving a player matches
 * @see https://docs.opendota.com/#tag/players%2Fpaths%2F~1players~1%7Baccount_id%7D~1matches%2Fget
 * @property match_id of the match
 * @property duration of the match
 * @property player_slot the player was in
 */
data class OpenDotaRecentMatchesJSON(
    val match_id: Long,
    val duration: Int,
    val player_slot: Int
)

/**
 * Result returned by a OpenDota endpoint, giving details about a match
 * @see https://docs.opendota.com/#tag/matches%2Fpaths%2F~1matches~1%7Bmatch_id%7D%2Fget
 * @property match_id of the match
 * @property players information about each player
 */
data class OpenDotaMatchJSON(
    val match_id: Long,
    val players: List<OpenDotaMatchPlayerJSON>
)

/**
 * A player information in a game
 * @see OpenDotaMatchJSON
 * @property player_slot the player was in during the game
 * @property account_id of the player
 */
data class OpenDotaMatchPlayerJSON(
    val player_slot: Int,
    val account_id: Long?
)
