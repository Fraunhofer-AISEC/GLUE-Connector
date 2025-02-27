package de.fraunhofer.iosb.odrl.evaluator

import de.fraunhofer.iosb.odrl.OdrlAsset

class OdrlAssetEvaluator {
    fun matches(
        ctx: Context,
        asset: OdrlAsset
    ): Result {
        return Result(
            ctx.params["targetUid"] == asset.uid,
            "Context uid ${ctx.params["targetUid"]} does not match asset uid ${asset.uid}"
        )
    }
}