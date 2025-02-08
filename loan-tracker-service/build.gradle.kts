import org.gradle.api.plugins.ApplicationPlugin

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.2"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.serialization") version "1.9.22" // Use the latest version
	application
	groovy
}

group = "com.zeenom"
version = "0.0.1-SNAPSHOT"

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.4")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("com.google.firebase:firebase-admin:9.2.0")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation("org.aspectj:aspectjweaver:1.9.7")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-security")

	// R2DBC reactive database setup
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
	runtimeOnly("io.r2dbc:r2dbc-pool")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Flyway database migration setup
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.postgresql:postgresql")

	//This is to mute some validator warnings
	implementation("org.hibernate.validator:hibernate-validator:8.0.1.Final")
	implementation("org.glassfish:jakarta.el:4.0.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
	testImplementation("org.spockframework:spock-core:2.3-groovy-3.0")
	testImplementation("org.spockframework:spock-spring:2.3-groovy-3.0")
	testImplementation("org.codehaus.groovy:groovy-all:3.0.9")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

application {
	mainClass.set("com.zeenom.loan_tracker.LoanTrackerApplication") // Change this to your main class
}
