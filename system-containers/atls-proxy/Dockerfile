FROM golang:1.25@sha256:294846130303560fa9cb1fcce26b0fb1c35007906abeac9419dce2fc13a07286 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags=-buildid= -tags nodefaults,socket,tpm,snp,sw .

# Some dependency seems to need cgo enabled, let's hope the image is still reproducible
FROM scratch
COPY --from=builder /src/atls-proxy /
EXPOSE 8080/tcp
ENTRYPOINT [ "/atls-proxy" ]
