FROM golang:1.26
ENV CMC_VERSION=5a1e3eb2d891a46d06ab917fc0e39d5f771456ef
RUN apt update && apt install -y moreutils golang-cfssl build-essential zlib1g-dev libssl-dev jq git wget curl
RUN git clone https://github.com/Fraunhofer-AISEC/cmc /src/cmc && git -C /src/cmc checkout ${CMC_VERSION}
WORKDIR /src/cmc
ENV GOBIN=/opt
RUN go build ./...
RUN go install ./...
ENTRYPOINT ["/bin/sh", "-c"]
WORKDIR /opt
