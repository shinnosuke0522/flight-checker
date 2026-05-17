package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.allure.AllureTestReporter

class ProjectConfig : AbstractProjectConfig() {
    override val extensions = listOf(
        AllureTestReporter()
    )
}
