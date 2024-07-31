package fhg.glue.odrl

fun main() {
    val odrlTest1 = "{\n" +
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

    val policy1 = OdrlPolicyParser().parseJsonLd(odrlTest1)
    val decision1 = policy1.evaluate(Context("http://www.w3.org/ns/odrl/2/play", "http://example.com/game:9090"))
    println("Decision 1 is $decision1")

    val odrlTest2 = "{\n" +
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

    val policy2 = OdrlPolicyParser().parseJsonLd(odrlTest2)
    val decision2 = policy2.evaluate(Context("http://www.w3.org/ns/odrl/2/distribute", "http://example.com/DataModel1"))
    println("Decision 2 is $decision2")
}