/*
 * Copyright 2025 Fraunhofer AISEC and Fraunhofer IOSB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        assertNotEquals(
            GlueValue(o),
            GlueValue(
                ObjectNode(
                    JsonNodeFactory.instance,
                    mapOf(
                        "key1" to TextNode("value1"),
                        "key2" to TextNode("_value2_"),
                    ),
                ),
            ),
        )
    }
}
