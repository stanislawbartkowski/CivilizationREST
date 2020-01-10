# CivilizationREST

## General information

It is the REST API service for Civilization Engine.<br>
The first part of HTTP URL is always *rest*.<br>

Example<br>
> http://\<server host\>:\<port number\>/rest/registerautom?autom=true


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
This CivRest API call initializes two players game and the oponent is automated player. The parameter specifies the civilizations engaged in the game. This call returns the game instance unique id.

| Info | Content
| -- | -- |
| URL | /civdata
| Request type | GET
| URL Params | what=8, obligatory
| URL Params | param=\<string\>, obligatory, the civilizations names seperated by coma, the second civilization is controlled by automated player.
| Success response | 200
| Response data | String, the unique game identifier
| Error response | Any other
| Sample call | curl -X GET http://localhost:8000/rest/civdata?what=8&param=China,Rome

