dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.coroutines.bom))

    implementation(libs.bundles.core)
    implementation(libs.koin.core)
}
