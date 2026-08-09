
plugins {
    alias(libs.plugins.quarkus.plugin)
}

dependencies {
    // BOM
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    implementation(enforcedPlatform(libs.quarkus.bom))
    // For Production
    implementation(libs.bundles.core)

    implementation(project(":domain"))

    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.coroutines.guava)

    // Quarkus
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.jackson)
    implementation(libs.quarkus.googlecloud.firestore)
    // For Test
    listOf(
        "testImplementation",
        "testFixturesImplementation",
    ).forEach { configuration ->
        add(configuration, platform(libs.kotest.bom))
        add(configuration, libs.bundles.quarkus.test.core)
    }
    // Quarkus Test
    testImplementation(libs.quarkus.junit5)
}

tasks.test { useJUnitPlatform() }

tasks.register<Test>("integrationTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.named("test"))
}
