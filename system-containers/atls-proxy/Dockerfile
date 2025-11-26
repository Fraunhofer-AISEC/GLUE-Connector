FROM golang:1.25@sha256:294846130303560fa9cb1fcce26b0fb1c35007906abeac9419dce2fc13a07286 AS builder
WORKDIR /src
COPY . .
RUN go build -trimpath -ldflags=-buildid= .

# Some dependency seems to need cgo enabled, let's hope the image is still reproducible
FROM gcr.io/distroless/base-debian12@sha256:9e9b50d2048db3741f86a48d939b4e4cc775f5889b3496439343301ff54cdba8
COPY --from=builder /src/atls-proxy /
EXPOSE 8080/tcp
ENTRYPOINT [ "/atls-proxy" ]
