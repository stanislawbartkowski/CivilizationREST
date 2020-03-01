# CivilizationREST

## General information

REST API service for Civilization Engine.<br>

Format:<br>
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
|LISTOFRES | 0 | List of resources<br>https://github.com/stanislawbartkowski/CivilizationEngine/wiki/GameObjects#resource-description
|REGISTEROWNER | 1| Initialize new single player, training game
|GETBOARD | 2 | Current board in JSON formar
|GETGAMES|3 | Get list of pending games ready to be resumed<br>https://github.com/stanislawbartkowski/CivilizationREST/blob/master/README.md#get-civilization-data-get-list-of-all-games
|UNREGISTERTOKEN|4| Unregister token, end of the game
|WAITINGGAMES|5| List of games in waiting
|TWOPLAYERSGAME|6| Initialize two players game, human to human
|GETJOURNAL|7|Get the player journal
|TWOPLAYERSGAMEWITHAUTOM|8| Initialize game with automated player
|SINGLEGAMEWITHAUTOM|9

### Initialize single player, training game
This CivRest API call return in JSON format a list of all games registered. 
| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=0, obligatory
| URL Params | Civilization name to start with
| Success response | 200
| Response data | Token
| Error response | Any other
| Sample call | curl -X GET "http://localhost:8000/rest/civdata?what=1&param=China"

### Get Civilization data, get list of all games
This CivRest API call return in JSON format a list of all games registered. 
| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=3, obligatory
| URL Params | can be ignored
| Success response | 200
| Response data | List of games in JSON format
| Error response | Any other
| Sample call | curl -X GET "http://localhost:8000/rest/civdata?what=3"

Sample output:
```JSON
[{"gameid":15,"civ":["China","Rome"],"createtime":1578832530775,"accesstime":1578833170421,"phase":"StartOfTurn","round":0,"endofgame":null},

{"gameid":14,"civ":["China","Rome"],"createtime":1578832423848,"accesstime":1578832519249,"phase":"StartOfTurn","round":0,"endofgame":null}

]
```

Output description
* gameid, the unique game identifier
* civ, the list of civilizations involved, single element in case of training game
* createtime, timestamp when data was created
* accesstime, timestamp when the game was played the last time
* phase, the phase of the game
* round, the round number
* endofgame, if not null, the game is completed

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

## All player are ready
This call checks if all players are registered. For two players game, it returns true if the second player has joined the game.

| Info | Content
| -- | -- |
| URL | /allready
| Request type | GET
| URL Params | token, obligatory, the token of the player who started or resumed the game. 
| Success response | 200
| Response data | string, 'true' if the second player joined or 'false' otherwise
| Error response | Any other
| Sample call | curl -X GET "http://localhost:8000/rest/allready?token=8jd6f1dpnl66dkk6mmr1eb266e

## Delete the game

This call deletes the game identified by *gameid*. The game is not available any longer. 

| Info | Content
| -- | -- |
| URL | /delete
| Request type | DELETE
| URL Params | gameid, integer, obligatory, the identifier of the game to remove
| Success response | 204
| Response data | nothing
| Error response | Any other. The call does not report any error if the game does not exist.
| Sample call | curl -X DELETE "http://localhost:8000/rest/delete?gameid=5"

## Resume the game

The call resume existing game and returns a new token for player. 

| Info | Content
| -- | -- |
| URL | /resumegame
| Request type | GET
| URL Params | gameid, integer, obligatory, the identifier of the game to remove
| URL Params | civ, string, obligatory, the civilization to play after resuming. The game can be two-players game and the player can choose the civilization to start with. For single player game, the civilization name
| Success response | 200
| Response data | token, gameid
| Error response | Any other. The call does not report any error if the game does not exist.
| Sample call | curl -X DELETE "http://localhost:8000/rest/resumegame?gameid=5,civ=China"

## Clear the waiting for automated player list.

This call is used only for testing

| Info | Content
| -- | -- |
| URL | /clearwaitinglist
| Request type | POST
| URL Params | no params
| Success response | 204
| Response data | nothing
| Error response | Any other.
| Sample call | curl -X POST "http://localhost:8000/rest/clearwaitinglist"

## Deploy the game

This call deploys the game board in JSON format and creates a new game.

| Info | Content
| -- | -- |
| URL | /deploygame
| Request type | POST
| URL Params | civ, string, obligatory, the list of civilization separated by coma participating in the game
| Success response | 204
| Response data | nothing
| Error response | Any other.
| Sample call | curl -X POST "http://localhost:8000/rest/deploygame?civ=China" -d /< JSON \>"

# Standalone server
The only dependency is simple RestService module.<br>
<br>
Download and install RestService to the local Maven repository.
https://github.com/stanislawbartkowski/RestService<br>
<br>
The RestService is using embedded Java HTTP Server, no additional dependency is required.<br>
https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html <br>
<br>

> git clone https://github.com/stanislawbartkowski/CivilizationREST.git<br>

## Customize

>cd CivilizationREST<br>
> cp template/rest.rc .<br>

Customize variables in rest.rc

| Variable | Description | Sample
| -------- | ----------- | ------
| PORT | Port number the server is listening | 8000
| REDISHOST | Host name of the Redis server | 192.168.0.206
| REDISPORT | Port number of the Redis server | 6379

## Build the server 

>cd CivilizationREST<br>
>./mvndeploy.sh<br>
>mvn package<br>

## Run the server
> ./run.sh
## Docker container
> ./createdocker.sh<br>

The container name is **civrest**
## Test
> curl -X GET http://localhost:8000/rest/civdata?what=0<br>
> curl -X GET "http://localhost:8000/rest/civdata?what=1&param=China"<br>

