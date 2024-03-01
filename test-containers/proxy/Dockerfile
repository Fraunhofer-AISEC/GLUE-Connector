FROM golang:1.22 as builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build .

FROM scratch
COPY --from=builder /src/proxy /
ENTRYPOINT [ "/proxy" ]
