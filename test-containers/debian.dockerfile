FROM debian:12
RUN apt-get update && apt-get install -y iputils-ping iproute2 netcat-openbsd
ENTRYPOINT [ "bash", "-ec", "sleep infinity" ]