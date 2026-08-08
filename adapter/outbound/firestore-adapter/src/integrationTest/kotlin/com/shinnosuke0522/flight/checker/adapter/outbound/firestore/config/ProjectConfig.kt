package com.shinnosuke0522.flight.checker.adapter.outbound.firestore.config

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(SpringExtension())
}
