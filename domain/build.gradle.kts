dependencies {
    // BOM
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))

    implementation(libs.bundles.core)
    testImplementation(libs.bundles.test.core)
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
}

tasks.test {
    useJUnitPlatform()
}