import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    // BOM
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(platform(libs.aws.bom))
    // For Production
    implementation(libs.bundles.core)
    implementation(libs.bundles.spring.boot.base)

    implementation(project(":domain"))
    implementation(project(":libs:aws"))

    implementation(libs.aws.sdk.dynamodb)
    implementation(libs.aws.sdk.dynamodb.enhanced)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.spring.tx)

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
}

tasks.withType<BootJar> {
    enabled = false
}
tasks.test { useJUnitPlatform() }
