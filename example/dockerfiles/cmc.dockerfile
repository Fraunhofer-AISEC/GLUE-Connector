FROM golang:1.26
ENV CMC_VERSION=v0.10.3
RUN apt update && apt install -y moreutils golang-cfssl build-essential zlib1g-dev libssl-dev jq git wget curl
RUN git clone https://github.com/Fraunhofer-AISEC/cmc /src/cmc && git -C /src/cmc checkout ${CMC_VERSION}
WORKDIR /src/cmc
ENV GOBIN=/opt
RUN go build ./cmcd ./tools/estserver ./tools/metatool
RUN go install ./cmcd ./tools/estserver ./tools/metatool
ENTRYPOINT ["/bin/sh", "-c"]
WORKDIR /opt
