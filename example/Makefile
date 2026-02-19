EXAMPLE = multi-stage

start: out/pki out/compose.yml out/images/atls-proxy.tar out/images/proxy.tar out/images/http-echo.tar metadata
	docker compose --project-directory . --file out/compose.yml up --build -d

stop: out/compose.yml
	docker compose --project-directory . --file out/compose.yml down -v

deploy-pipeline:
	echo Not implemented
	exit 1

.PHONY: start stop deploy-pipeline

#
# Dependencies/Implementation Details
#

out:
	mkdir out

out/compose.yml: out j2render $(EXAMPLE)-values.json templates/compose.yml.j2
	./j2render templates/compose.yml.j2 "$(EXAMPLE)-values.json" > out/compose.yml

# Container Images

out/images: out
	mkdir -p out/images

out/images/atls-proxy.tar: out/images $(shell find ../system-containers/atls-proxy -type f)
	docker build -t container-pipeline/atls-proxy ../system-containers/atls-proxy
	docker save container-pipeline/atls-proxy > out/images/atls-proxy.tar

out/images/proxy.tar: out/images $(shell find ../test-containers/proxy -type f)
	docker build -t container-pipeline/proxy ../test-containers/proxy
	docker save container-pipeline/proxy > out/images/proxy.tar

out/images/http-echo.tar: out/images $(shell find ../test-containers/http-echo -type f)
	docker build -t container-pipeline/http-echo ../test-containers/http-echo
	docker save container-pipeline/http-echo > out/images/http-echo.tar

# PKI setup

out/pki: out generate-pki.sh
	./generate-pki.sh

# Metadata templates

out/metadata-templates: out
	mkdir -p out/metadata-templates

# Mark this as phony so we always regenerate this on start
.PHONY: out/metadata-templates

out/metadata-templates/%: templates/%.j2 out/metadata-templates j2render
	./j2render $< "$(EXAMPLE)-values.json" > $@

metadata: out/metadata-templates/rtm.manifest.json out/metadata-templates/image.manifest.json out/metadata-templates/image.description.json

.PHONY: metadata