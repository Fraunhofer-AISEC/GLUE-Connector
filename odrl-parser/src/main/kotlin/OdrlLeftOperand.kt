package fhg.glue.odrl

import java.time.LocalDateTime

class OdrlLeftOperand(val uid: String)
{
    fun eval(): OdrlLiteral<*> {
        when (uid) {
            OdrlIdentifiers.DATETIME -> return OdrlLiteral<LocalDateTime>(LocalDateTime.now())
        }

        // absolutePosition, absoluteSize, absoluteSpatialPosition, absoluteTemporalPosition, count, dateTime, delayPeriod, deliveryChannel, device, elapsedTime, event, fileFormat, industry, language, media, meteredTime, payAmount, percentage, product, purpose, recipient, relativePosition, relativeSize, relativeSpatialPosition, relativeTemporalPosition, resolution, spatial, spatialCoordinates, system, systemDevice, timeInterval, unitOfCount, version, virtualLocation

        throw OdrlException("Unsupported left operand $uid")
    }
}