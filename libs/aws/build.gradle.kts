import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.aws.bom))

    implementation(libs.bundles.core)
    implementation(libs.bundles.spring.boot.base)

    implementation(libs.aws.sdk.core)
    implementation(libs.aws.sdk.auth)
    implementation(libs.aws.sdk.regions)
}

tasks.withType<BootJar> {
    enabled = false
}
