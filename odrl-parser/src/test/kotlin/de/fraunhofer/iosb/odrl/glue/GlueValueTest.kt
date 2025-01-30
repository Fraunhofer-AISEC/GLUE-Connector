package de.fraunhofer.iosb.odrl.glue

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GlueValueTest {
    @Test
    fun testGlueValueEquality() {
        assertEquals(GlueValue(BooleanNode.TRUE), GlueValue(BooleanNode.TRUE))
        assertNotEquals(GlueValue(BooleanNode.FALSE), GlueValue(BooleanNode.TRUE))

        assertEquals(GlueValue(IntNode(42)), GlueValue(IntNode(42)))
        assertNotEquals(GlueValue(IntNode(0)), GlueValue(IntNode(42)))

        assertEquals(GlueValue(LongNode(Long.MAX_VALUE)), GlueValue(LongNode(Long.MAX_VALUE)))
        assertNotEquals(GlueValue(LongNode(-42)), GlueValue(LongNode(42)))

        assertEquals(GlueValue(TextNode("test")), GlueValue(TextNode("test")))
        assertNotEquals(GlueValue(TextNode("test")), GlueValue(TextNode("\"test\"")))
        assertNotEquals(GlueValue(TextNode(" ")), GlueValue(TextNode("")))

        val a = ArrayNode(JsonNodeFactory.instance, listOf(IntNode(42), IntNode(43)))
        assertEquals(GlueValue(a), GlueValue(a))
        assertNotEquals(GlueValue(a), GlueValue(ArrayNode(JsonNodeFactory.instance, listOf(IntNode(42), IntNode(44)))))

        val o = ObjectNode(JsonNodeFactory.instance, mapOf("key1" to TextNode("value1"), "key2" to TextNode("value2")))
        assertEquals(GlueValue(o), GlueValue(o))
        assertNotEquals(GlueValue(o), GlueValue(ObjectNode(JsonNodeFactory.instance, mapOf("key1" to TextNode("value1"), "key2" to TextNode("_value2_")))))
    }
}