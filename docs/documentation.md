# Documentation

## Compilation of Project Documentation

Use these commands to compile the project documentation for local usage.

```bash
docker compose -f docker-compose.docs.yml run --rm mkdocs build
```
The resulting files are placed in site/.

## Automatically publish to GitHub Pages

Create .github/workflows/documentation.yml:
```
name: Publish Documentation

on:
push:
branches:
- main
paths:
- "docs/**"
- "mkdocs.yml"
- ".github/workflows/documentation.yml"
workflow_dispatch:

permissions:
contents: write

jobs:
deploy:
runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install MkDocs
        run: pip install "mkdocs-material==9.*"

      - name: Deploy to GitHub Pages
        run: mkdocs gh-deploy --force
```

This builds the documentation and pushes the generated website to the gh-pages branch.
