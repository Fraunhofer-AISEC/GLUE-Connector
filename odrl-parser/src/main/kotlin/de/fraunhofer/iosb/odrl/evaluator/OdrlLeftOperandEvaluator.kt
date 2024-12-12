package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.*
import de.fraunhofer.iosb.odrl.glue.GlueContainerPipeline
import de.fraunhofer.iosb.odrl.glue.GlueIdentifiers
import java.time.LocalDateTime

class OdrlLeftOperandEvaluator {
    fun evaluate(
        ctx: Context,
        leftOperand: OdrlLeftOperand
    ): OdrlRightOperand {
        when (leftOperand.uid.toString()) {
            OdrlIdentifiers.DATETIME -> return XmlDateTime(LocalDateTime.now())
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