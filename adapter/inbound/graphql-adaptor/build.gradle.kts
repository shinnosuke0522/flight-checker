import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.dgs.codegen)
}

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))
    // Dependencies
    implementation(libs.bundles.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.boot.graphql.starter)
    implementation(libs.graphql.extended.validation)
    testImplementation(libs.bundles.test.core)
    testImplementation(libs.kotest.assertions.arrow)
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
    schemaPaths = mutableListOf("${projectDir}/src/main/resources/graphql")
    packageName = "com.shinnosuke0522.flight.checker.adapter.inbound.graphql"
    subPackageNameTypes = "model"
    language = "kotlin"
    generateClient = true
    typeMapping = mutableMapOf(
        "Date" to "java.time.LocalDate",
        "DateTime" to "java.time.OffsetDateTime",
        "UserId" to "java.lang.String"
    )
}

tasks.withType<Jar> {
    enabled = true
}

tasks.withType<BootJar> {
    enabled = false
}
