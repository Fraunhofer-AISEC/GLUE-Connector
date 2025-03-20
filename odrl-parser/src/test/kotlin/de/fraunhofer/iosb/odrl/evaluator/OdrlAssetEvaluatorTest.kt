package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAsset
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OdrlAssetEvaluatorTest {
    private val evaluator = OdrlAssetEvaluator()
    private val asset1 = OdrlAsset(
        uid = URI.create("http://example.com/asset")
    )

    @Test
    fun testAssetEvaluation() {
        assertTrue(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/asset"), asset1
            ).success
        )
        assertFalse(
            evaluator.matches(
                Context("http://example.com/action", "http://example.com/anotherAsset"), asset1
            ).success
        )
    }
}