FROM --platform=$BUILDPLATFORM golang:1.26@sha256:3aff6657219a4d9c14e27fb1d8976c49c29fddb70ba835014f477e1c70636647 AS builder
ARG TARGETPLATFORM
ARG BUILDPLATFORM
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 GOOS=${TARGETPLATFORM%/*} GOARCH=${TARGETPLATFORM#*/} go build -trimpath -ldflags=-buildid= .

FROM scratch
COPY --from=builder /src/proxy /
EXPOSE 8080/tcp
CMD [ "/proxy" ]
