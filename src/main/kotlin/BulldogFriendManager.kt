import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.time.Duration
import kotlin.math.abs

/** Main entrypoint of the program */
fun main(args: Array<String>) = BulldogFriendManager(accountId = 92832630).run()

/** Logger used in the program, log to chat */
private val logger = KotlinLogging.logger {}

/**
 * Main class of the program. Discover all games played the last 30 days and update nicknnames accordingly
 * @property accountId AccountID to use for the 30 days game detection
 * @property gameCounts Number of game played per SteamID in the last 30 days
 * @property nicknameRegex Regex of a MC nickname, ex: "3MCSepitys". "xx" means private stat profile
 */
class BulldogFriendManager(val accountId: Long) {

    private var gameCounts: HashMap<Long, Int> = hashMapOf()
    private var nicknameRegex: Regex = """(\d|\d\d|xx)MC(.+)""".toRegex()

    /**
     * Fetch the history of games played in the last 30 days.
     * Uses OpenDota, fills gameCounts
     */
    private suspend fun fetchGameHistory() {
        // HTTP Client with rate limiter function
        val client = HttpClient(CIO) {
            install(JsonFeature)
        }
        val openDotaRateLimiterConfig =
            RateLimiterConfig.custom().limitRefreshPeriod(Duration.ofSeconds(1)).limitForPeriod(1)
                .timeoutDuration(Duration.ofMillis(1001)).build()
        val openDotaRateLimiterRegistry = RateLimiterRegistry.of(openDotaRateLimiterConfig)
        val openDotaRateLimiter = openDotaRateLimiterRegistry.rateLimiter("openDota")

        // Fetch games played < 30 days
        var recentMatches: List<OpenDotaRecentMatchesJSON> = listOf()
        openDotaRateLimiter.executeSuspendFunction {
            recentMatches = client.get("https://api.opendota.com/api/players/$accountId/matches?date=30")
        }
        logger.info { "OpenDota processing ${recentMatches.size} matches" }

        // Compute games played per SteamID
        for ((count, game) in recentMatches.withIndex()) {
            logger.info { "O> Game ${count+1}/${recentMatches.size} import" }
            // Skip games shorter than 10 min
            if (game.duration < 600) {
                continue
            }

            var gameInformation = OpenDotaMatchJSON(0, listOf())
            openDotaRateLimiter.executeSuspendFunction {
                gameInformation = client.get("https://api.opendota.com/api/matches/${game.match_id}")
            }
            for (player in gameInformation.players) {
                // Skip if the player is not in the same team, or it's the Bulldog account
                if (abs(player.player_slot - game.player_slot) > 5 || player.account_id == null || player.account_id == accountId) {
                    continue
                }

                // Count the game
                gameCounts[player.account_id] = gameCounts.getOrDefault(player.account_id, 0) + 1
            }
        }

        client.close()
    }

    /** Entrypoint of the program */
    fun run() {
        runBlocking {
            // Start Steam Bot
            val steamBotCoroutine = async { SteamBot.run() }

            // Fetch players history
            fetchGameHistory()

            // Modify nicknames
            SteamBot.waitForNicknames()
            for (nickname in SteamBot.nicknames) {
                val matchResult = nicknameRegex.matchEntire(nickname.nickname) ?: continue

                val (oldGameCount, name) = matchResult.destructured
                val newValue = gameCounts.getOrDefault(nickname.steamID.accountID, 0)
                val newGameCount = if (newValue < 10) "0$newValue" else "$newValue"
                if (oldGameCount != newGameCount)
                    SteamBot.modifyNickname(nickname.steamID, nickname.nickname, "${newGameCount}MC$name")
            }
            SteamBot.waitForNicknameChanges()

            // Stop Steam Bot
            SteamBot.stop()
            steamBotCoroutine.await()
        }
    }
}
