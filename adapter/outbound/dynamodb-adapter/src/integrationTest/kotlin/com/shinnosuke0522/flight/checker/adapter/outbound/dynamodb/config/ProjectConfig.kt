package com.shinnosuke0522.flight.checker.adapter.outbound.dynamodb.config

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.extensions.spring.SpringExtension

class ProjectConfig : AbstractProjectConfig() {
    override val extensions: List<Extension> = listOf(SpringExtension())
}
