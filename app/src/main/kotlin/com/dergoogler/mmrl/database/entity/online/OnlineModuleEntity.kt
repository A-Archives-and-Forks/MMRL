package com.dergoogler.mmrl.database.entity.online

import androidx.room.Embedded
import androidx.room.Entity
import com.dergoogler.mmrl.model.online.Blacklist
import com.dergoogler.mmrl.model.online.OnlineModule

@Entity(tableName = "onlineModules", primaryKeys = ["id", "repoUrl"])
data class OnlineModuleEntity(
    val id: String,
    val repoUrl: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val author: String,
    val description: String,

    val maxApi: Int? = null,
    val minApi: Int? = null,

    val size: Int? = null,
    val categories: List<String>? = null,
    val icon: String? = null,
    val homepage: String? = null,
    val donate: String? = null,
    val support: String? = null,
    val cover: String? = null,
    val screenshots: List<String>? = null,
    val license: String? = "",
    val readme: String? = null,
    val verified: Boolean? = null,

    val require: List<String>? = null,
    val devices: List<String>? = null,
    val arch: List<String>? = null,

    @Embedded val manager: ModuleManagerEntity? = null,
    @Embedded val root: ModuleRootEntity? = null,
    @Embedded val note: ModuleNoteEntity? = null,
    @Embedded val features: ModuleFeaturesEntity? = null,
    @Embedded val track: TrackJsonEntity,
    @Embedded val blacklist: BlacklistEntity?
) {
    constructor(
        original: OnlineModule,
        repoUrl: String,
        blacklist: Blacklist
    ) : this(
        id = original.id,
        repoUrl = repoUrl,
        name = original.name,
        version = original.version,
        versionCode = original.versionCode,
        author = original.author,
        description = original.description,
        track = TrackJsonEntity(original.track),
        note = ModuleNoteEntity(original.note),
        root = ModuleRootEntity(original.root),
        features = ModuleFeaturesEntity(original.features),
        maxApi = original.maxApi,
        minApi = original.minApi,
        size = original.size,
        categories = original.categories,
        icon = original.icon,
        homepage = original.homepage,
        donate = original.donate,
        support = original.support,
        cover = original.cover,
        screenshots = original.screenshots,
        license = original.license,
        readme = original.readme,
        verified = original.verified,
        manager = ModuleManagerEntity(original.manager),

        require = original.require,
        devices = original.devices,
        arch = original.arch,
        blacklist = BlacklistEntity(blacklist)
    )

    fun toModule() = OnlineModule(
        id = id,
        name = name,
        version = version,
        versionCode = versionCode,
        author = author,
        description = description,
        track = track.toTrack(),
        note = note?.toNote(),
        root = root?.toRoot(),
        features = features?.toFeatures(),
        versions = listOf(),
        maxApi = maxApi,
        minApi = minApi,
        size = size,
        categories = categories,
        icon = icon,
        homepage = homepage,
        donate = donate,
        support = support,
        cover = cover,
        screenshots = screenshots,
        license = license,
        readme = readme,
        verified = verified,
        manager = manager?.toManager(),

        require = require,
        devices = devices,
        arch = arch,
        blacklist = blacklist?.toBlacklist()
    )
}