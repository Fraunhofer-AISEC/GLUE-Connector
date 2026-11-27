# Documentation

The project documentation you are currently reading is part of the GLUE Connector repository on GitHub.
Its source code resiedes in the [`docs/` directory](https://github.com/Fraunhofer-AISEC/GLUE-Connector/docs).
When documentation changes are merged into main, the documentation page is updated automatically by GitHub Actions.

## Compilation of Project Documentation

As with any code contributions, changes to the documentation must be built and tested before contributing.
The following command may be used to compile the documentation and place the rendered outputs under `site/`

```bash
docker compose -f docker-compose.docs.yml run --rm mkdocs build
```

The following file compose file must be present at the repository root as `docker-compose.docs.yml`.

```yaml
name: glue-documentation.md

services:
  mkdocs:
    image: squidfunk/mkdocs-material:9
    ports:
      - "8000:8000"
    volumes:
      - ./:/docs
    command: serve --dev-addr=0.0.0.0:8000
```
