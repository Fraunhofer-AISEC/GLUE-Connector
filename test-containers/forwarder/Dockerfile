FROM golang:1.22@sha256:7761eeedd113a5751a7e1c135c89c4656a661ad73065dd09035ed3770b063c19 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags=-buildid= .

FROM alpine@sha256:1e42bbe2508154c9126d48c2b8a75420c3544343bf86fd041fb7527e017a4b4a
COPY --from=builder /src/forwarder /
EXPOSE 8080/tcp
ENTRYPOINT [ "/forwarder" ]
CMD [ "http://final-connector" ]