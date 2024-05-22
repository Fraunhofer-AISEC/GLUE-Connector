## HTTP-Server Example Usage

### Create demo pipeline:
```sh
curl --request POST \
  --url http://localhost:8080/contract \
  --header 'Content-Type: application/json' \
  --data '{
    "pipeline": {
	    "containers": [
		    {
			    "name": "proxy",
			    "image": "milux/proxy",
			    "cmd": ["8080", "http-echo:8080"]
		    },{
			    "name": "http-echo",
			    "image": "milux/http-echo",
			    "cmd": ["0.0.0.0:8080"]
		    }
	    ]
	}
}'
```
The server will return the contract ID as a response once the pipeline is started.

### Stop/Delete demo pipeline
```sh
curl --request DELETE \
  --url http://localhost:8080/contract/<contract id> \
```