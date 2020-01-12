# CivilizationREST

## General information

It is the REST API service for Civilization Engine.<br>
The first part of HTTP URL is always *rest*.<br>

Example<br>
> http://\<server host\>:\<port number\>/rest/registerautom?autom=true

## Automated player

The CivilizationREST allows engagement of automated player.<br>
At the beginning, the automated player should register itself using "Register automation" API. It is only the signal "I'm up and ready". The signal unlocks "Get Civilization data/start two players with automated" API call.<br>
<br>
To start the automated game, the following sequence should happen.<br.
* The human or another automated player should launch "Get Civilization data/start two players with automated" API call. It returns the token of the player, human or another automated if machine to machine game is going to be started. The new game is started and waiting for another player to join.
* The automated player should monitor "Automated player, get waiting game id.". If empty, no game is waiting. If not empty, the waiting game id is extracted and removed from the local cache and returned. 
* The player can scan the list of all waiting games using "Get list of games waiting for another player to join in" and extract the civilization name.
* Then the player should join the game using "Join game" API call using proper *gameid* and *civ* parameter. The call returns the player token.
* Having received the token, the player can play the new or resumed game.

## Register automation
This API call informs CivREST that automated player is up and ready to play. After this call, the CivREST unblocks "automated player" feature. Otherwise, only training or human to human game is allowed.

| Info | Content
| -- | -- |
| URL | /registerautom
| Request type | PUT
| URL Params | autom=true, obligatory
| Success response | 204, no data
| Error response | Any other
| Sample call | curl -X PUT http://localhost:8000/rest/registerautom?autom=true

## Get Civilization data
This is a general purpose CivRest API call to get different type of data related to the game. The parameter *what* determines what specific data is requested and there is an optional query parameter *param* in JSON format. The result is brought back also in JSON format.

| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=\<number\>, obligatory, specifies the data to be extracted, the number 0-8.
| URL Params | param=\<string\>, optional, depending on the *what*, additional parameter if necessary
| Success response | 200
| Response data | The requested data in JSON format or string
| Error response | Any other
| Sample call | curl -X GET http://localhost:8000/rest/civdata?what=8&param=China,Rome

The following data (*what* parameter) are possible.

| Id |  Number | Content
| -- | -- | -- | 
|LISTOFRES | 0 | List of resources
|REGISTEROWNER | 1|
|GETBOARD | 2 | Current board in JSON formar
|GETGAMES|3 |
|UNREGISTERTOKEN|4| Unregister token, end of the game
|WAITINGGAMES|5| List of games in waiting
|TWOPLAYERSGAME|6| Initialize two players game, human to human
|GETJOURNAL|7|Get the player journal
|TWOPLAYERSGAMEWITHAUTOM|8| Initialize game with automated player
|SINGLEGAMEWITHAUTOM|9

### Get Civilization data, start two players game with automated player
This CivRest API call initializes two players game and the oponent is automated player. The parameter specifies the civilizations engaged in the game. This call returns the first player token. The game id is cached in the CivilizationREST and can be extracted by "Automated player, get game id".

| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=8, obligatory
| URL Params | param=\<string\>, obligatory, the civilizations names seperated by coma, the second civilization is controlled by automated player.
| Success response | 200
| Response data | String, the unique game identifier
| Error response | Any other
| Sample call | curl -X GET "http://localhost:8000/rest/civdata?what=8&param=China,Rome"

### Get list of games waiting for another player to join in
This API call returns a list of games waiting for another player to join in. The player, human or machine, should pick up the game and join. The list can be empty if there is no game in waiting.

| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=5, obligatory
| Success response | 200 or 204
| Response data | List of waiting games in JSON format or empty string (code 204) if there is no waiting games
| Error response | Any other
| Sample call | curl -X GET "http://localhost:8000/rest/civdata?what=5"

Sample output<br>

```JSON
[
{"gameid":9,"createtime":1578742625922,"players":["China"],"waiting":["Rome"]},
{"gameid":8,"createtime":1578742617068,"players":["China"],"waiting":["Rome"]}
]
```
Single game:<br>
* gameid, number, unique game identifier
* createtime, timestamp when game was created
* players, the civilization already registered in this game and waiting for the opponent
* waiting, the civilization waiting for the player

## Automated player, get game id waiting for automated player.
This API is designed for automated player. It returns the id of the game waiting for automated player to pick up. The empty id means that there is no waiting game. 

| Info | Content
| -- | -- |
| URL | /getwaiting
| Request type | GET
| URL Params | no any params
| Success response | 200 or 204
| Response data | String, if empty then there is no waiting list (code 204), if not empty, the game id waiting for automated player (code 200).
| Error response | Any other
| Sample call | curl -X GET http://localhost:8000/rest/getwaiting

## Join game
This API call allows to join new or resumed game as the second player. The game should be the one of the games on the list of waiting games ("Get list of games waiting for another player to join in"). If successfull, the call return the token of the player.

| Info | Content
| -- | -- |
| URL | /joingame
| Request type | POST
| URL Params | no any params
| Success response | 200
| Response data | String, the token of the player
| Error response | Any other
| Sample call | curl -X POST "http://localhost:8000/rest/joingame?gameid=5&civ=China"

## Itemize command
This API call takes the list of all currently possible variants of the command. 

| Info | Content
| -- | -- |
| URL | /itemize
| Request type | GET
| URL Params | token : obligatory, the player token
| URL Params | command: obligatory, command to itemize
| Success response | 200
| Response data | JSON, list of possible variants of the command
| Error response | Any other
| Sample call | curl -X POST "http://localhost:8000/rest/itemize?token=xxxxx&command=SETCAPITAL"

## Execute command
Executes command in the current game. The player is identified by the token received from "Get Civilization data, start two players game with automated player" or "Join game". The (row,column) identifies the boards square. If the command/action is not dependent on it, pass (-1,-1). The *jsparam* provided command parameter. It's format and content depend on the command.

| Info | Content
| -- | -- |
| URL | /command
| Request type | POST
| URL Params | token : obligatory, the player token
| URL Params | action : obligatory, command to execute
| URL Params | row: obligatory, integer, the row where command is going to be executed, if the command does not require it, -1
| URL Params | column: obligatory, integer, the column where command is going to be executed, if the command does not require it, -1
| URL Params | jsparam: optional, the command params in JSON format, the content depends on the action. If the command does not require any additonal parameters, the *jsparam* can be ignored
| Success response | 200
| Response data | Empty string if success. If not empty, error occured and the reponse contains the error message.
| Error response | Any other
| Sample call | curl -X POST "http://localhost:8000/rest/itemize?token=8jd6f1dpnl66dkk6mmr1eb266e&action=SETCAPITAL&row=1&col=2"


