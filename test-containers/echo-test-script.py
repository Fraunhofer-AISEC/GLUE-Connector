#!/usr/bin/env python3
import http.client
import sys
import time

if len(sys.argv) < 3:
    print(f"Usage: {sys.argv[0]} <host> <port>")
    sys.exit(1)

host = sys.argv[1]
port = int(sys.argv[2])
payload = "Test"
headers = {"Content-Type": "application/x-www-form-urlencoded"}

conn = http.client.HTTPConnection(host, port)

for i in range(3):
    if i > 0:
        time.sleep(1)

    start = time.perf_counter()
    try:
        conn.request("POST", "/echo", payload, headers)
        resp = conn.getresponse()
        received = resp.read().decode('utf-8')
        duration = time.perf_counter() - start

        is_match = (received == payload)
        status_msg = "OK" if is_match else "FAIL"

        print(f"Req {i+1}: {status_msg} (Status {resp.status}) - {duration:.5f}s")

        if not is_match:
            print(f" -> Expected: '{payload}', Got: '{received}'")
    except Exception as e:
        print(f"Req {i+1}: Error - {e}")

conn.close()