import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties

/**
 * Configuration singleton used in the application
 * Loaded from ressources files, environment variables and system properties
 * @property config configuration loaded using the key options
 * @property steam_login_key options of the steam.login key
 * @property steam_password_key options of the steam.password key
 * @property steamLogin login for the steam client
 * @property steamPassword for the steam client
 */
object Configuration {
    private val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("settings.properties") overriding
            ConfigurationProperties.fromResource("settings.example.properties")

    private val steam_login_key = Key("steam.login", stringType)
    private val steam_password_key = Key("steam.password", stringType)

    val steamLogin: String by lazy { config[steam_login_key] }
    val steamPassword: String by lazy { config[steam_password_key] }
}
