import org.gradle.kotlin.dsl.withType

plugins {
    alias(libs.plugins.dgs.codegen)
    alias(libs.plugins.quarkus.plugin)
}

dependencies {
    // BOM
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))
    // Dependencies
    implementation(libs.bundles.core)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.graphql.extended.validation)

    // Quarkus
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.jackson)
    implementation(libs.quarkus.smallrye.graphql)
    testImplementation(libs.bundles.test.core)

    // Quarkus Test
    testImplementation(libs.quarkus.junit5)
    testFixturesImplementation(libs.quarkus.junit5)
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
    schemaPaths = mutableListOf("${projectDir}/src/main/resources/graphql")
    packageName = "com.shinnosuke0522.flight.checker.adapter.inbound.graphql"
    subPackageNameTypes = "model"
    language = "kotlin"
    generateClient = false
    typeMapping = mutableMapOf(
        "Date" to "java.time.LocalDate",
        "DateTime" to "java.time.OffsetDateTime",
        "UserId" to "java.lang.String"
    )
}

tasks.withType<Jar> {
    enabled = true
}
