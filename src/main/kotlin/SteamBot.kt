import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PlayerNickname
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.delay
import mu.KotlinLogging

/** Logger used in the program, log to chat */
private val logger = KotlinLogging.logger {}

/**
 * Singleton managing access to Steam Network features
 * @property steamClient JavaSteam client
 * @property manager Callback manager for Steam events
 * @property steamUser Functions to manage the Steam user
 * @property steamFriends Functions to manage the Steam friends
 * @property isRunning Indicates if the bot should stop or not
 * @property fetchedNicknames Indicates if the nicknames where retrieved from the network
 * @property nicknames Nicknames fetched from the network
 * @property modifiedNicknames Number of nicknames whose callback are awaited
 */
object SteamBot {

    private val steamClient = SteamClient()
    private var manager = CallbackManager(steamClient)
    private var steamUser: SteamUser = steamClient.getHandler(SteamUser::class.java)
    private var steamFriends: SteamFriends = steamClient.getHandler(SteamFriends::class.java)

    private var isRunning: Boolean = true

    var fetchedNicknames: Boolean = false
    var nicknames: List<PlayerNickname> = listOf()
    var modifiedNicknames: Int = 0

    // Setup all the callbacks
    init {
        manager.subscribe(ConnectedCallback::class.java, this::onConnected)
        manager.subscribe(DisconnectedCallback::class.java, this::onDisconnected)
        manager.subscribe(LoggedOnCallback::class.java, this::onLoggedOn)
        manager.subscribe(LoggedOffCallback::class.java, this::onLoggedOff)
        manager.subscribe(FriendsListCallback::class.java, this::onFriendList)
        manager.subscribe(NicknameListCallback::class.java, this::onNicknameList)
        manager.subscribe(NicknameCallback::class.java, this::onNickname)
    }

    /**
     * Main loop of the program, use it in a dedicated thread/process/coroutine
     */
    suspend fun run() {
        logger.info { "Starting Steam client" }
        isRunning = true
        steamClient.connect()

        while (isRunning) {
            // in order for the callbacks to get routed, they need to be handled by the manager
            manager.runWaitCallbacks(500L)
            delay(50)
        }
    }

    /**
     * Stop the connection to steam and the bot
     */
    fun stop() {
        steamClient.disconnect()
    }

    /**
     * Callback when the client is connected to Steam
     */
    private fun onConnected(callback: ConnectedCallback) {
        logger.info { "S> Connected to Steam!" }
        val details = LogOnDetails()
        details.username = Configuration.steamLogin
        details.password = Configuration.steamPassword

        details.loginID = 149
        steamUser.logOn(details)
    }

    /**
     * Callback when the client is disconnected from Steam
     */
    private fun onDisconnected(callback: DisconnectedCallback) {
        logger.info { "S> Disconnected from Steam" }
        isRunning = false
    }

    /**
     * Callback when the client is logged in the user account
     */
    private fun onLoggedOn(callback: LoggedOnCallback) {
        if (callback.result != EResult.OK) {
            logger.info { "S> Unable to logon to Steam: ${callback.result}" }
            isRunning = false
            return
        }
        logger.info { "S> Successfully logged on!" }
    }

    /**
     * Callback when the client is logged off the user acocunt
     */
    private fun onLoggedOff(callback: LoggedOffCallback) {
        logger.info { "S> Logged off of Steam: ${callback.result}" }
        isRunning = false
    }

    /**
     * Callback when the user friend list is retrieved by the client
     */
    private fun onFriendList(callback: FriendsListCallback) {
        // at this point, the client has received the friend list
        logger.info { "S> Friends received" }
    }

    /**
     * Callback when the user nickname list is retrieved by the client
     */
    private fun onNicknameList(callback: NicknameListCallback) {
        // at this point, the client has received the nicknames
        logger.info { "S> Nicknames received" }
        fetchedNicknames = true
        nicknames = callback.nicknames
    }

    /**
     * Wait for the nicknames to be retrieved by the client
     */
    suspend fun waitForNicknames() {
        while(!fetchedNicknames && isRunning) {
            delay(1000L)
        }
    }

    /**
     * Send a nickname change request to Steam
     */
    fun modifyNickname(steamID: SteamID, oldNickname: String, newNickname: String) {
        logger.info { "S> Nickname: ${steamID.accountID} : $oldNickname -> $newNickname" }
        modifiedNicknames++
        steamFriends.setFriendNickname(steamID, newNickname)
    }

    /**
     * Callback when a nickname change has been processed
     */
    private fun onNickname(callback: NicknameCallback) {
        logger.info { "S> Nicknames change accepted" }
        modifiedNicknames -= 1
    }

    /**
     * Wait for all the nickname changes to be processed by the Steam network
     */
    suspend fun waitForNicknameChanges() {
        while(modifiedNicknames!=0 && isRunning) {
            delay(1000L)
        }
    }
}
