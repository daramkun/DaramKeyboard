package com.daram.keyboard.model

import `in`.daram.nutcracker.prediction.AmbiguityResolver
import `in`.daram.nutcracker.prediction.KeyMapper
import `in`.daram.nutcracker.prediction.mapper.CheonjiinKeyMapper
import `in`.daram.nutcracker.prediction.mapper.DanmoemKeyMapper
import `in`.daram.nutcracker.prediction.mapper.DubeolsikKeyMapper
import `in`.daram.nutcracker.prediction.mapper.MotorolaKeyMapper
import `in`.daram.nutcracker.prediction.mapper.NaratgeulKeyMapper
import `in`.daram.nutcracker.prediction.mapper.SkyIIKeyMapper
import `in`.daram.nutcracker.prediction.resolver.CheonjiinAmbiguityResolver
import `in`.daram.nutcracker.prediction.resolver.DanmoemAmbiguityResolver
import `in`.daram.nutcracker.prediction.resolver.MotorolaAmbiguityResolver
import `in`.daram.nutcracker.prediction.resolver.SkyIIAmbiguityResolver

enum class KoreanKeyboardType(val prefValue: String, val displayName: String) {
    NARATGEUL("naratgeul", "나랏글"),
    CHEONJIIN("cheonjiin", "천지인"),
    SKY_II("sky2", "SKY-II"),
    MOTOROLA("motorola", "모토로라"),
    DANMOEUM("danmoeum", "단모음"),
    DUBEOLSIK("dubeolsik", "두벌식");

    fun keyMapper(): KeyMapper = when (this) {
        NARATGEUL -> NaratgeulKeyMapper()
        CHEONJIIN -> CheonjiinKeyMapper()
        SKY_II    -> SkyIIKeyMapper()
        MOTOROLA  -> MotorolaKeyMapper()
        DANMOEUM  -> DanmoemKeyMapper()
        DUBEOLSIK -> DubeolsikKeyMapper()
    }

    fun ambiguityResolver(): AmbiguityResolver? = when (this) {
        CHEONJIIN -> CheonjiinAmbiguityResolver()
        SKY_II    -> SkyIIAmbiguityResolver()
        MOTOROLA  -> MotorolaAmbiguityResolver()
        DANMOEUM  -> DanmoemAmbiguityResolver()
        else      -> null
    }
}
