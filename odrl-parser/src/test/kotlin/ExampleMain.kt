import de.fraunhofer.iosb.odrl.evaluator.OdrlPolicyEvaluatorTest

fun main() {
    val tester = OdrlPolicyEvaluatorTest()

    tester.testPolicyExample1()
    tester.testPolicyExample2()
    tester.testPolicyExample3()

    // policy-example-4.json only works with glue-odrl.jsonld in the file system root (e.g. C:\)
    //tester.testPolicyExample4()
}