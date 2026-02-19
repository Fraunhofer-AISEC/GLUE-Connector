FROM debian:stable
RUN apt-get update && apt-get install -y swtpm swtpm-tools
ENTRYPOINT ["/bin/sh", "-c"]