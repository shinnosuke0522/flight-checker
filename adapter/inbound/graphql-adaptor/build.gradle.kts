plugins {
    alias(libs.plugins.dgs.codegen)
}

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.spring.cloud.dependencies))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))
    // Dependencies
    implementation(libs.bundles.core)
    testImplementation(libs.bundles.test.core)
    testImplementation(libs.kotest.assertions.arrow)
}

tasks.withType<com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask> {
    schemaPaths = mutableListOf("${projectDir}/src/main/resources/graphql")
    packageName = "com.shinnosuke0522.flight.checker.adapter.inbound.graphql"
    subPackageNameTypes = "model"
    language = "kotlin"
    generateClient = true
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    enabled = false
}

tasks.withType<Jar> {
    enabled = true
}
