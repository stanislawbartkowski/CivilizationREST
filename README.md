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
* The human or another automated player should launch "Get Civilization data/start two players with automated" API call. It returns the id of the player, human or another automated player. The second game id is waiting if the CivilizationREST local cache.
* The automated player should monitor "Automated player, get waiting player id.". If empty, nobody is waiting. If not empty, the second game waiting id is extracted and removed from the local cache and returned. The two player game can start, the first player is the player which launched "Get Civilization data/start two players with automated", the second player is the owner of "Automated player, get waiting player id."

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

### Get Civilization data, start two players game with automated player
This CivRest API call initializes two players game and the oponent is automated player. The parameter specifies the civilizations engaged in the game. This call returns the first player id. The second player id is cached in the CivilizationREST and can be extracted by "Automated player, get waiting player id.".

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

## Automated player, get waiting player id.
This API is designed for automated player. It returns the id of the game waiting for automated player to pick up. If the id s empty, it means that there is no waiting game.
| Info | Content
| -- | -- |
| URL | /getwaiting
| Request type | GET
| URL Params | no any params
| Success response | 200
| Response data | String, if empty then there is no waiting list, if not empty, the second player id.
| Error response | Any other
| Sample call | curl -X GET http://localhost:8000/rest/getwaiting

