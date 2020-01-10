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
