EXAMPLE = multi-stage

start: out/pki out/compose.yml metadata
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