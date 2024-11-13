FROM golang:1.22 as builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags=-buildid= .

FROM scratch
COPY --from=builder /src/proxy /
ENTRYPOINT [ "/proxy" ]
