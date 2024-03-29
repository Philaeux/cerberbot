import asyncio
import aiohttp

import discord

from cerberbot.cfg import cfg
from cerberbot.database.database import Database


class CerberBot:

    def __init__(self):
        self.streamer_id = 92832630
        self.cfg = cfg
        self.database = Database(check_migrations=True)

        self.intents = discord.Intents.default()
        self.intents.messages = True
        self.intents.message_content = True
        self.client = DiscordClient(intents=self.intents)

    def run(self):
        self.client.run(self.cfg['Default']['DISCORD_TOKEN'])

    async def fetch_player_history(self):
        game_counts = {}
        async with aiohttp.ClientSession() as session:
            async with session.get(
                    f"https://api.opendota.com/api/players/{self.streamer_id}/matches?date=30") as response:
                game_list = await response.json()
                print(f"Detected {len(game_list)} games in the past 30 days.")

        headers = {'Content-Type': 'application/json',
                   'Authorization': f"Bearer {self.cfg['Default']['STRATZ_TOKEN']}"}
        async with aiohttp.ClientSession(headers=headers) as session:
            for index, game in enumerate(game_list):
                print(f"> Game {index} processing")
                # Skip games shorter than 10 min
                if game['duration'] < 600:
                    continue

                await asyncio.sleep(1)  # Avoid OpenDota 429
                params = {
                    'query': "{ match(id: " + str(game['match_id']) + ") { players { steamAccountId playerSlot }} }"}
                async with session.get(f"https://api.stratz.com/graphql", params=params) as response:
                    details = await response.json()
                    match = details["data"]["match"]
                    if "players" not in match:
                        continue
                    for player in match['players']:
                        # Skip if the player is not in the same team, or it's the Bulldog account
                        if ('steamAccountId' not in player
                                or player['steamAccountId'] is None
                                or player['steamAccountId'] == self.streamer_id
                                or abs(player['playerSlot'] - game['player_slot']) > 5):
                            continue

                        if player['steamAccountId'] not in game_counts:
                            game_counts[player['steamAccountId']] = GameCount(player['steamAccountId'])
                        else:
                            game_counts[player['steamAccountId']].count = game_counts[
                                                                              player['steamAccountId']].count + 1

        sorted_counts = sorted(game_counts.values(), key=lambda x: x.count, reverse=True)
        for player in list(sorted_counts):
            print(f"{player.count} games for {player.id}")
        return sorted_counts


class GameCount:
    def __init__(self, id):
        self.id = id
        self.count = 1


class DiscordClient(discord.Client):
    async def on_ready(self):
        print(f'We have logged in as {self.user}')

    async def on_message(self, message):
        if message.author == self.user:
            return

        if message.content.startswith('!addme'):
            await message.channel.send('SOON')
