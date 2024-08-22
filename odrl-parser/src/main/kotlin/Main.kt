package fhg.glue.odrl

fun main() {
    val simpleDateTimePolicy = "{\n" +
            "   \"@context\": \"http://www.w3.org/ns/odrl.jsonld\",\n" +
            "   \"@type\": \"Policy\",\n" +
            "   \"uid\": \"http://example.com/policy:9090\",\n" +
            "   \"profile\": \"http://example.com/odrl:profile:07\",\n" +
            "   \"permission\": [{\n" +
            "       \"target\": \"http://example.com/game:9090\",\n" +
            "       \"assigner\": \"http://example.com/org:xyz\",\n" +
            "       \"action\": \"play\",\n" +
            "       \"constraint\": [{\n" +
            "           \"leftOperand\": \"dateTime\",\n" +
            "           \"operator\": \"lteq\",\n" +
            "           \"rightOperand\": { \"@value\": \"2025-12-31\", \"@type\": \"xsd:date\" }\n" +
            "       }]\n" +
            "   }]\n" +
            "}";

    var decision = OdrlPolicyParser().parseJsonLd(simpleDateTimePolicy).evaluate(
        Context("http://www.w3.org/ns/odrl/2/play", "http://example.com/game:9090"))
    println("Decision of simpleDateTimePolicy is '$decision' (should be: true)")

    val extendedDateTimePolicy = "{\n" +
            "  \"@context\": {\n" +
            "    \"rdf\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n" +
            "    \"rdfs\": \"http://www.w3.org/2000/01/rdf-schema#\",\n" +
            "    \"xs\": \"http://www.w3.org/2001/XMLSchema#\",\n" +
            "    \"odrl\": \"http://www.w3.org/ns/odrl/2/\"\n" +
            "  },\n" +
            "  \"@id\": \"https://www.aisec.fraunhofer.com/odrl-distribute-to-printer\",\n" +
            "  \"rdf:type\": {\n" +
            "    \"@id\": \"odrl:Policy\"\n" +
            "  },\n" +
            "  \"odrl:uid\": \"37b0b888-6cd0-4162-a08a-32640c1da7d5\",\n" +
            "  \"odrl:permission\": {\n" +
            "    \"rdf:type\": {\n" +
            "      \"@id\": \"odrl:Permission\"\n" +
            "    },\n" +
            "    \"odrl:target\": \"http://example.com/DataModel1\",\n" +
            "    \"odrl:assignee\": \"http://example.com/intermediaryParty\",\n" +
            "    \"odrl:action\": \"http://www.w3.org/ns/odrl/2/distribute\",\n" +
            "    \"odrl:constraint\": [\n" +
            "      {\n" +
            "        \"@type\": \"odrl:Constraint\",\n" +
            "        \"odrl:leftOperand\": \"http://www.w3.org/ns/odrl/2/dateTime\",\n" +
            "        \"odrl:operator\": \"http://www.w3.org/ns/odrl/2/lteq\",\n" +
            "        \"odrl:rightOperand\": {\n" +
            "          \"rdf:value\": \"2015-12-31\",\n" +
            "          \"rdf:type\": {\n" +
            "            \"@id\": \"xs:date\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"odrl:dataType\": {\n" +
            "          \"@id\": \"xs:date\"\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}\n"

    decision = OdrlPolicyParser().parseJsonLd(extendedDateTimePolicy).evaluate(
        Context("http://www.w3.org/ns/odrl/2/distribute", "http://example.com/DataModel1"))
    println("Decision of extendedDateTimePolicy is '$decision' (should be: false)\n")

    val simplePipelinePolicy = "{\n" +
            "    \"@context\": {\n" +
            "      \"odrl\": \"http://www.w3.org/ns/odrl/2/\",\n" +
            "      \"glue\": \"http://fhg.de/glue/\"\n" +
            "    },\n" +
            "    \"@id\": \"http://dataowner.com/policy:6163\",\n" +
            "    \"odrl:permission\": [{\n" +
            "        \"odrl:target\": \"http://dataowner.com/document:1234\",\n" +
            "        \"odrl:assigner\": \"http://dataowner.com/org\",\n" +
            "        \"odrl:assignee\": \"http://dataconsumer.com/org\",\n" +
            "        \"odrl:action\": {\n" +
            "            \"@id\": \"http://www.w3.org/ns/odrl/2/use\",\n" +
            "            \"odrl:refinement\": [{\n" +
            "                \"odrl:leftOperand\": \"http://fhg.de/glue/containerPipeline\",\n" +
            "                \"odrl:operator\": \"http://www.w3.org/ns/odrl/2/eq\",\n" +
            "                \"odrl:rightOperand\": {\n" +
            "                    \"@type\": \"http://fhg.de/glue/ContainerPipeline\",\n" +
            "                    \"glue:container\": [{\n" +
            "                        \"@type\": \"http://fhg.de/glue/Container\",\n" +
            "                        \"glue:image\": \"test/container1\",\n" +
            "                        \"glue:hash\": \"364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e\",\n" +
            "                        \"glue:entrypoint\": \"/app\",\n" +
            "                        \"glue:allowNetwork\": true\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"@type\": \"http://fhg.de/glue/Container\",\n" +
            "                        \"glue:image\": \"test/container2\",\n" +
            "                        \"glue:hash\": \"ac924fdd26993b2ad157e3512c4a36ad293491f6eb8d268d6d4e1bf1c5c307ad\"\n" +
            "                    }]\n" +
            "                }\n" +
            "            }]\n" +
            "        },\n" +
            "        \"odrl:constraint\": [{\n" +
            "            \"odrl:leftOperand\": \"http://www.w3.org/ns/odrl/2/dateTime\",\n" +
            "            \"odrl:operator\": \"http://www.w3.org/ns/odrl/2/lteq\",\n" +
            "            \"odrl:rightOperand\": {\"@value\": \"2025-12-21\", \"@type\": \"http://www.w3.org/2001/XMLSchema#date\"}\n" +
            "        }]\n" +
            "    },\n" +
            "    {\n" +
            "        \"odrl:target\": \"http://dataowner.com/document:1234\",\n" +
            "        \"odrl:assigner\": \"http://dataowner.com/org\",\n" +
            "        \"odrl:assignee\": \"http://dataconsumer.com/org\",\n" +
            "        \"odrl:action\": \"http://www.w3.org/ns/odrl/2/distribute\",\n" +
            "        \"odrl:duty\": {\n" +
            "            \"odrl:action\": {\n" +
            "                \"@id\": \"http://www.w3.org/ns/odrl/2/nextPolicy\",\n" +
            "                \"odrl:refinement\": [{\n" +
            "                    \"odrl:leftOperand\": \"http://fhg.de/glue/containerPipeline\",\n" +
            "                    \"odrl:operator\": \"http://www.w3.org/ns/odrl/2/eq\",\n" +
            "                    \"odrl:rightOperand\": {\n" +
            "                        \"@type\": \"http://fhg.de/glue/ContainerPipeline\",\n" +
            "                        \"glue:container\": {\n" +
            "                            \"@type\": \"http://fhg.de/glue/Container\",\n" +
            "                            \"glue:image\": \"test/policyCompiler\",\n" +
            "                            \"glue:hash\": \"5e7ef9594b0b96c5f591dc9d3462943e340941c770e5e3885a1864b998de016f\",\n" +
            "                            \"glue:entrypoint\": \"/app --template=http://dataowner.com/policyTemplate:1234\",\n" +
            "                            \"glue:allowNetwork\": true\n" +
            "                        }\n" +
            "                    }\n" +
            "                }]\n" +
            "            }\n" +
            "        }\n" +
            "    }]\n" +
            "}\n"

    val legitimatePipeline = GlueContainerPipeline(listOf(
        GlueContainer("test/container1", "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e", "/app", true),
        GlueContainer("test/container2", "ac924fdd26993b2ad157e3512c4a36ad293491f6eb8d268d6d4e1bf1c5c307ad")))
    decision = OdrlPolicyParser().parseJsonLd(simplePipelinePolicy).evaluate(
        Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234", legitimatePipeline))
    println("Decision of simplePipelinePolicy with a legitimate pipeline is '$decision' (should be: true)")

    val maliciousPipeline = GlueContainerPipeline(listOf(
        GlueContainer("test/container1", "364a4787748a98c1c28085d4154a06c3a9be9a67c0b08a56b3f69fa8fabf172e", "/app", true),
        GlueContainer("test/container2", "someotherhashvaluesomeotherhashvaluesomeotherhashvaluesomeotherh")))
    decision = OdrlPolicyParser().parseJsonLd(simplePipelinePolicy).evaluate(
        Context("http://www.w3.org/ns/odrl/2/use", "http://dataowner.com/document:1234", maliciousPipeline))
    println("Decision of simplePipelinePolicy with a malicious pipeline is '$decision' (should be: false)")

    val onActionCallback = { action: OdrlAction ->
        if (action.uid == "http://www.w3.org/ns/odrl/2/nextPolicy") {
            println("   | Launching policy pipeline " + action.refinements[0].rightOperand.value)
            println("   | Deploying policy template at remote data consumer...")
            true
        } else {
            false
        }
    }
    decision = OdrlPolicyParser().parseJsonLd(simplePipelinePolicy).evaluate(
        Context("http://www.w3.org/ns/odrl/2/distribute", "http://dataowner.com/document:1234"), onActionCallback)
    println("Decision of simplePipelinePolicy when distributing asset is '$decision' (should be: true)")
}