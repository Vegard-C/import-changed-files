buildscript {
  repositories {
    mavenCentral()
  }
}

val sqlliteVersion: String by project
val kotlinVersion: String by project

plugins {
  kotlin("jvm")
  kotlin("plugin.spring")
  kotlin("plugin.allopen")
  kotlin("plugin.noarg")
  id("org.springframework.boot")
  id("io.spring.dependency-management")
}

group = "com." + project.name
version = "0.0.1"

java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
  implementation("org.springframework.boot:spring-boot-starter")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName<Jar>("jar") {
  enabled = false
}

tasks.withType<Test> {
  useJUnitPlatform()
}