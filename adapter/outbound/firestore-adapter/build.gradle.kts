import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(platform(libs.spring.cloud.gcp.dependencies))
    implementation(enforcedPlatform(libs.quarkus.bom))
    // For Production
    implementation(libs.bundles.core)
    implementation(libs.bundles.spring.boot.base)

    implementation(project(":domain"))

    implementation(libs.spring.cloud.gcp.starter.data.firestore)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.tx)
    implementation(libs.kotlinx.coroutines.guava)

    // Quarkus
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.jackson)
    implementation(libs.quarkus.googlecloud.firestore)
    // For Test
    listOf(
        "testImplementation",
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, platform(libs.kotest.bom))
        add(configuration, libs.bundles.test.core)
    }
    listOf(
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, libs.bundles.container.test.base)
    }
    
    testFixturesApi(libs.testcontainers.gcloud)

    // Quarkus Test
    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.kotest.extensions.quarkus)
}

tasks.withType<BootJar> {
    enabled = false
}
tasks.test { useJUnitPlatform() }
