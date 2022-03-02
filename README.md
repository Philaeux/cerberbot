# bulldog-friends
Bot to manage the Steam friend list of Bulldog subs.

## Architecture
The bot is made using ``Kotlin``, and runs on JVM 16. The program fetches the game information of the past 30 days using ``OpenDota``. It then updates the Steam friend list of a specified account using ``JavaSteam``.

## Build and Run
The application is build using gradle. You need to set up the running options (Steam account login & password). You can create a properties file at `/src/main/resources/settings.properties` similar to the one at `/src/main/resources/settings.example.properties`, or use environment variables.

Run the program with
```
gradle run
```

Make a distribution ready program with
```
gradle installdist
```
