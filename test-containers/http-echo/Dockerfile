FROM golang:1.22 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags=-buildid= .

FROM scratch
COPY --from=builder /src/http-echo /
ENTRYPOINT [ "/http-echo" ]
CMD [ "0.0.0.0:8080" ]

