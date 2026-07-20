dependencies {
    // BOM
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))
    testImplementation(platform(libs.kotest.bom))
    // Dependencies
    implementation(libs.bundles.core)
    testImplementation(libs.bundles.test.core)
}

tasks.test {
    useJUnitPlatform()
}
