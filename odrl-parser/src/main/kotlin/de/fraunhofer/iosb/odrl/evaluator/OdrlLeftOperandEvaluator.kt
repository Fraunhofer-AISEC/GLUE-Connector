/*
 * Copyright 2024 Fraunhofer AISEC and Fraunhofer IOSB
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
package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import java.time.LocalDateTime

class OdrlLeftOperandEvaluator {
    fun evaluate(
        ctx: Context,
        leftOperand: OdrlLeftOperand,
    ): OdrlRightOperand {
        when (leftOperand.uid.toString()) {
            OdrlIdentifiers.DATETIME -> {
                return XmlDateTime(LocalDateTime.now())
            }

            GlueIdentifiers.HAS_CONTAINER_PIPELINE -> {
                val pipeline = ctx.params["pipeline"]
                if (pipeline != null && pipeline is GlueContainerPipeline) return pipeline
                throw OdrlException("Context is missing a GlueContainerPipeline in field 'pipeline'")
            }
        }

        // absolutePosition, absoluteSize, absoluteSpatialPosition, absoluteTemporalPosition, count, delayPeriod,
        // deliveryChannel, device, elapsedTime, event, fileFormat, industry, language, media, meteredTime, payAmount,
        // percentage, product, purpose, recipient, relativePosition, relativeSize, relativeSpatialPosition,
        // relativeTemporalPosition, resolution, spatial, spatialCoordinates, system, systemDevice, timeInterval,
        // unitOfCount, version, virtualLocation

        throw OdrlException("Unsupported left operand $leftOperand")
    }
}
