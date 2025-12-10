FROM golang:1.22@sha256:7761eeedd113a5751a7e1c135c89c4656a661ad73065dd09035ed3770b063c19 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -trimpath -ldflags=-buildid= .

FROM scratch
COPY --from=builder /src/forwarder /
EXPOSE 8080/tcp
ENTRYPOINT [ "/forwarder" ]
CMD [ "http://connector3" ]