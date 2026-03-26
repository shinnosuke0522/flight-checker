plugins {
	alias(libs.plugins.detekt.plugin) apply false
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.spring.boot) apply false
	alias(libs.plugins.spring.dependency.management) apply false
}

allprojects {
	repositories {
		mavenCentral()
		maven { url = uri("https://repo.spring.io/snapshot") }
	}
}

subprojects {
	group = "com.github.shinnosuke0522"
	version = "0.0.1-SNAPSHOT"

	// Version Catalog は rootProject から参照する
	val libs = rootProject.libs

	//==================================
	// Common
	//==================================

	// Plugin
	apply(plugin = libs.plugins.detekt.plugin.get().pluginId)
	apply(plugin = libs.plugins.kotlin.jvm.get().pluginId)
	apply(plugin = "java-test-fixtures")

	// Detekt Formatting
	dependencies {
		add("detektPlugins", libs.detekt.formatting)
	}

	configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
		// Detekt に関する設定ファイル
		config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
		// 並列処理
		parallel = true
		// 自動修正
		autoCorrect = true
		// デフォルト設定の上に自分の設定ファイルを適用する
		buildUponDefaultConfig = true
		// レポートファイルに出力されるファイルパスのベースとなる
		basePath = rootDir.absolutePath
	}

	// Java
	extensions.configure<JavaPluginExtension> {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(21))
		}
	}

	//==================================
	// App & Infra
	//==================================
	if (path.startsWith(":app") || path.startsWith(":infra")) {
		apply(plugin = libs.plugins.kotlin.spring.get().pluginId)
		apply(plugin = libs.plugins.spring.boot.get().pluginId)
		apply(plugin = libs.plugins.spring.dependency.management.get().pluginId)

		apply(plugin = "jvm-test-suite")

		@Suppress("UnstableApiUsage")
		configure<TestingExtension> {
			suites {
				val test by getting(org.gradle.api.plugins.jvm.JvmTestSuite::class) {
					useJUnitJupiter()
				}
			}
		}
	}
}
