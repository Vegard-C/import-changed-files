rootProject.name = "import-changed-files"

pluginManagement {
  val kotlinVersion: String by settings
  val springBootVersion: String by settings
  val springDependencyVersion: String by settings

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }

  plugins {
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    kotlin("plugin.noarg") version kotlinVersion
    id("org.springframework.boot") version springBootVersion
    id("io.spring.dependency-management") version springDependencyVersion
  }
}
