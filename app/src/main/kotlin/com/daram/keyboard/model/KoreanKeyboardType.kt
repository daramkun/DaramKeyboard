package com.daram.keyboard.model

import com.daram.nutcracker.prediction.AmbiguityResolver
import com.daram.nutcracker.prediction.KeyMapper
import com.daram.nutcracker.prediction.mapper.CheonjiinKeyMapper
import com.daram.nutcracker.prediction.mapper.DanmoemKeyMapper
import com.daram.nutcracker.prediction.mapper.DubeolsikKeyMapper
import com.daram.nutcracker.prediction.mapper.MotorolaKeyMapper
import com.daram.nutcracker.prediction.mapper.Mue128KeyMapper
import com.daram.nutcracker.prediction.mapper.NaratgeulKeyMapper
import com.daram.nutcracker.prediction.mapper.SkyIIKeyMapper
import com.daram.nutcracker.prediction.resolver.CheonjiinAmbiguityResolver
import com.daram.nutcracker.prediction.resolver.DanmoemAmbiguityResolver
import com.daram.nutcracker.prediction.resolver.MotorolaAmbiguityResolver
import com.daram.nutcracker.prediction.resolver.Mue128AmbiguityResolver
import com.daram.nutcracker.prediction.resolver.SkyIIAmbiguityResolver

enum class KoreanKeyboardType(val prefValue: String, val displayName: String) {
    NARATGEUL("naratgeul", "나랏글"),
    CHEONJIIN("cheonjiin", "천지인"),
    SKY_II("sky2", "SKY-II"),
    MOTOROLA("motorola", "모토로라"),
    DANMOEUM("danmoeum", "단모음"),
    DUBEOLSIK("dubeolsik", "두벌식"),
    MUE128("mue128", "무이128");

    fun keyMapper(): KeyMapper = when (this) {
        NARATGEUL -> NaratgeulKeyMapper()
        CHEONJIIN -> CheonjiinKeyMapper()
        SKY_II    -> SkyIIKeyMapper()
        MOTOROLA  -> MotorolaKeyMapper()
        DANMOEUM  -> DanmoemKeyMapper()
        DUBEOLSIK -> DubeolsikKeyMapper()
        MUE128    -> Mue128KeyMapper()
    }

    fun ambiguityResolver(): AmbiguityResolver? = when (this) {
        CHEONJIIN -> CheonjiinAmbiguityResolver()
        SKY_II    -> SkyIIAmbiguityResolver()
        MOTOROLA  -> MotorolaAmbiguityResolver()
        DANMOEUM  -> DanmoemAmbiguityResolver()
        MUE128    -> Mue128AmbiguityResolver()
        else      -> null
    }
}
