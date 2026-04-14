FROM golang:1.26
ENV CMC_VERSION=5a5085b9a86a20f241c0f69d8cf599d1bd23751e
RUN apt update && apt install -y moreutils golang-cfssl build-essential zlib1g-dev libssl-dev jq git wget curl
RUN git clone https://github.com/Fraunhofer-AISEC/cmc /src/cmc && git -C /src/cmc checkout ${CMC_VERSION}
WORKDIR /src/cmc
ENV GOBIN=/opt
RUN go build ./...
RUN go install ./...
ENTRYPOINT ["/bin/sh", "-c"]
WORKDIR /opt
