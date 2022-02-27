import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

class DogFriends(val accountId: Long) {

    /** Number of game played per SteamID in the last 30 days*/
    private var gameCounts: HashMap<Long, Int> = hashMapOf()

    /**
     * Fetch the history of games played in the last 30 days.
     * Uses OpenDota, fills gameCounts
     */
    private suspend fun fetchGameHistory() {
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val openDotaRateLimiterConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofMillis(1001))
            .build()
        val openDotaRateLimiterRegistry = RateLimiterRegistry.of(openDotaRateLimiterConfig)
        val openDotaRateLimiter = openDotaRateLimiterRegistry.rateLimiter("openDota")


        // Fetch games played < 30 days
        var recentMatches: List<OpenDotaRecentMatchesJSON> = listOf()
        openDotaRateLimiter.executeSuspendFunction {
            recentMatches = client.get("https://api.opendota.com/api/players/$accountId/matches?date=30")
        }
        logger.info { "Processing ${recentMatches.size} matches." }

        // Compute games played per SteamID
        var count = 0
        for (game in recentMatches) {
            count++
            logger.info { "   Game $count/${recentMatches.size}" }
            // Skip games shorter than 10 min
            if (game.duration < 600) {
                continue
            }

            var gameInformation: OpenDotaMatchJSON = OpenDotaMatchJSON(0, listOf())
            openDotaRateLimiter.executeSuspendFunction {
                gameInformation = client.get("https://api.opendota.com/api/matches/${game.match_id}")
            }
            for (player in gameInformation.players) {
                // Skip if the player is not in the same team
                // or it's the Bulldog account
                if (abs(player.player_slot - game.player_slot) > 5 || player.account_id == null || player.account_id == accountId) {
                    continue
                }

                // Count the game
                gameCounts[player.account_id] = gameCounts.getOrDefault(player.account_id, 0) + 1
            }
        }

        client.close()
    }

    fun run() {
        runBlocking {
            fetchGameHistory()
        }

        for (player in gameCounts.toList().sortedBy { (_, value) -> value}.toMap()) {
            println("${player.key} ${player.value}")
        }
    }
}
