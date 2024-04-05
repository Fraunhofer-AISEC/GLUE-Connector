## HTTP-Server Example Usage

### Create demo pipeline:
```
curl --request POST \
  --url http://localhost:8080/pipeline \
  --header 'Content-Type: application/json' \
  --data '{
	"id": "test",
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
}'
```

### Stop/Delete demo pipeline
```
curl --request DELETE \
  --url http://localhost:8080/pipeline/test \
```