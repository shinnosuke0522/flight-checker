import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    configurations.all {
        exclude(group = "io.kotest", module = "kotest-extensions-spring")
        exclude(group = "org.springframework")
        exclude(group = "org.springframework.boot")
    }

    // BOM
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))

    // For Production
    implementation(libs.bundles.core)
    implementation(project(":domain"))
    implementation(project(":libs:gcp"))

    implementation(libs.google.cloud.firestore.official)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.guava)

    implementation(libs.koin.core)

    // For Test
    listOf(
        "testImplementation",
        "integrationTestImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, platform(libs.kotest.bom))
        add(configuration, libs.bundles.test.core)
    }

    testFixturesApi(libs.testcontainers.gcloud)
    testFixturesApi(libs.testcontainers.core)
    testFixturesApi(libs.koin.test)
}

tasks.withType<BootJar> {
    enabled = false
}
tasks.test { useJUnitPlatform() }
