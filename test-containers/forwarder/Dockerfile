FROM --platform=$BUILDPLATFORM golang:1.27@sha256:f20d83a83e18dbd2a4714b8ffd4b30b287b6243962dd75608e86abcc63ba4efc AS builder
ARG TARGETPLATFORM
ARG BUILDPLATFORM
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 GOOS=${TARGETPLATFORM%/*} GOARCH=${TARGETPLATFORM#*/} go build -trimpath -ldflags=-buildid= .

FROM scratch
COPY --from=builder /src/forwarder /
EXPOSE 8080/tcp
ENTRYPOINT [ "/forwarder" ]
