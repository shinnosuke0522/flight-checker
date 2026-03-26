plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.graalvm.native)
}

group = "com.github.shinnosuke0522"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// BOM
	implementation(platform(libs.kotlin.bom))
	implementation(platform(libs.aws.bom))
	implementation(platform(libs.spring.cloud.dependencies))

	implementation(libs.spring.boot.starter.restclient)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.webflux)
	implementation(libs.reactor.kotlin.extensions)
	implementation(libs.kotlin.reflect)
	implementation(libs.kotlinx.coroutines.reactor)
	implementation(libs.spring.cloud.function.web)
	implementation(libs.spring.cloud.starter.circuitbreaker.reactor.resilience4j)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.aws.sdk.dynamodb)

	// lambda用
	implementation(libs.spring.cloud.function.context)
	implementation(libs.spring.cloud.function.adapter.aws)
	implementation(libs.aws.lambda.java.core)
	implementation(libs.aws.lambda.java.events)

	developmentOnly(libs.spring.boot.docker.compose)

	testImplementation(libs.spring.boot.starter.restclient.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(libs.spring.boot.starter.webflux.test)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.kotlinx.coroutines.test)

	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.spring.boot.testcontainers)

	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
