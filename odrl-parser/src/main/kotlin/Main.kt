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
            "           \"rightOperand\": { \"@value\": \"2024-12-31\", \"@type\": \"xsd:date\" }\n" +
            "       }]\n" +
            "   }]\n" +
            "}";

    val policy = OdrlPolicyParser().parseJson(odrlTest1)
    val decision = policy.evaluate(Context("play", "http://example.com/game:9090"))
    println("Decision is $decision")
}