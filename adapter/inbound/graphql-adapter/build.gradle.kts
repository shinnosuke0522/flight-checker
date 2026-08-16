import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))
    implementation(libs.bundles.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.graphql.kotlin.ktor.server)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    testImplementation(libs.bundles.test.core)
}

tasks.withType<Jar> {
    enabled = true
}

tasks.withType<BootJar> {
    enabled = false
}
