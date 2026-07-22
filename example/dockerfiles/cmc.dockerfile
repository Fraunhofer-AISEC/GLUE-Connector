FROM golang:1.26
ENV CMC_VERSION=a8c8f3c6f449175b3240803710d349bdd28e756e
RUN apt update && apt install -y moreutils golang-cfssl build-essential zlib1g-dev libssl-dev jq git wget curl
RUN git clone https://github.com/Fraunhofer-AISEC/cmc /src/cmc && git -C /src/cmc checkout ${CMC_VERSION}
WORKDIR /src/cmc
ENV GOBIN=/opt
RUN go build ./cmcd ./provision/estserver ./tools/metatool
RUN go install ./cmcd ./provision/estserver ./tools/metatool
ENTRYPOINT ["/bin/sh", "-c"]
WORKDIR /opt
