# Network Exposure

This page describes which ports a GLUE connector opens and how other connectors reach the pipelines it runs.
It also lists the options that set the public addresses of both.

## Connector Ports

| Port   | Purpose                                                                                     |
|--------|---------------------------------------------------------------------------------------------|
| `8080` | User-facing API for starting negotiations and managing pipelines. Do not expose it publicly. |
| `8444` | Inter-connector (DSP) API for negotiations between connectors, secured with (attested) TLS. |

Other connectors only need to reach port `8444`, at the address set in `connector.publicUrl`.

## Configuration

| Option                   | Environment variable | Default             | Description                                                                |
|--------------------------|----------------------|---------------------|----------------------------------------------------------------------------|
| `connector.publicUrl`    | `PUBLIC_URL`         | local IP address    | Public URL of the inter-connector API.                                     |
| `connector.pipelineHost` | `PIPELINE_HOST`      | host of `publicUrl` | Host under which other connectors reach this connector's pipelines.        |
| `sniProxy.enabled`       | `SNI_PROXY_ENABLED`  | `false`             | Expose pipelines through an SNI proxy instead of publishing their ports.   |
| `sniProxy.port`          | `SNI_PROXY_PORT`     | port of `publicUrl` | Public port of the SNI proxy that is announced to other connectors.        |
| `docker.sniProxyNetwork` | `SNI_PROXY_NETWORK`  | `sni-proxy`         | Docker network shared by the SNI proxy and the pipeline entry containers.  |

## Pipeline Exposure

Every pipeline has an entry container that listens on port `8080` inside the container.
This port is exposed in one of two ways.

### Port Publishing (default)

Docker publishes the entry port on a free host port from the kernel's ephemeral port range
(`net.ipv4.ip_local_port_range`, usually `32768-60999`).
Other connectors reach the pipeline at `<pipelineHost>:<port>`, so the whole ephemeral range must be reachable.

### SNI Proxy

With `sniProxy.enabled`, a TLS passthrough proxy that routes by SNI, such as Envoy, HAProxy or Traefik, fronts all
pipelines on a single port.
Every pipeline gets the hostname `<pipelineId>.<pipelineHost>`. Other connectors connect to
`<pipelineId>.<pipelineHost>:<sniProxy.port>`, which requires a wildcard DNS record for `*.<pipelineHost>`.
The proxy resolves the same hostname on the network `docker.sniProxyNetwork` and forwards the connection to the
entry container's port `8080`.
`sniProxy.port` is only the port announced to other connectors. It does not have to match the port the proxy itself
listens on, for example when the proxy is behind a port forwarding.

## Appendix: Minimal Envoy Configuration

This configuration assumes `connector.publicUrl=https://glue.example.org:8444` (and therefore the default
`connector.pipelineHost=glue.example.org`) and an Envoy container attached to `docker.sniProxyNetwork`.
The Envoy container must also be able to reach the connector under the name `connector`.

```yaml
static_resources:
  listeners:
    - name: tls
      address:
        socket_address: { address: 0.0.0.0, port_value: 8444 }
      listener_filters:
        - name: envoy.filters.listener.tls_inspector
          typed_config:
            "@type": type.googleapis.com/envoy.extensions.filters.listener.tls_inspector.v3.TlsInspector
      filter_chains:
        - filter_chain_match:
            server_names: ["glue.example.org"]
          filters:
            - name: envoy.filters.network.tcp_proxy
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.tcp_proxy.v3.TcpProxy
                stat_prefix: connector
                cluster: connector
        - filter_chain_match:
            server_names: ["*.glue.example.org"]
          filters:
            - name: envoy.filters.network.sni_dynamic_forward_proxy
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.sni_dynamic_forward_proxy.v3.FilterConfig
                port_value: 8080
                dns_cache_config: { name: pipelines }
            - name: envoy.filters.network.tcp_proxy
              typed_config:
                "@type": type.googleapis.com/envoy.extensions.filters.network.tcp_proxy.v3.TcpProxy
                stat_prefix: pipelines
                cluster: pipelines
  clusters:
    - name: connector
      type: STRICT_DNS
      load_assignment:
        cluster_name: connector
        endpoints:
          - lb_endpoints:
              - endpoint:
                  address:
                    socket_address: { address: connector, port_value: 8444 }
    - name: pipelines
      lb_policy: CLUSTER_PROVIDED
      cluster_type:
        name: envoy.clusters.dynamic_forward_proxy
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.clusters.dynamic_forward_proxy.v3.ClusterConfig
          dns_cache_config: { name: pipelines }
```
