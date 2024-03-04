import com.google.protobuf.gradle.id

plugins {
  kotlin("jvm") version "1.9.10"
  application

  id("com.google.protobuf") version "0.9.1"
}

repositories {
  mavenCentral()
  maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

val restateVersion = "0.8.0-SNAPSHOT"

dependencies {
  // Restate SDK
  implementation("dev.restate:sdk-api-kotlin:$restateVersion")
  implementation("dev.restate:sdk-http-vertx:$restateVersion")
  // To use Jackson to read/write state entries (optional)
  implementation("dev.restate:sdk-serde-jackson:$restateVersion")

  // Protobuf and grpc dependencies
  implementation("com.google.protobuf:protobuf-java:3.24.3")
  implementation("io.grpc:grpc-stub:1.58.0")
  implementation("io.grpc:grpc-protobuf:1.58.0")
  // This is needed to compile the @Generated annotation forced by the grpc compiler
  // See https://github.com/grpc/grpc-java/issues/9153
  compileOnly("org.apache.tomcat:annotations-api:6.0.53")

  // Logging (optional)
  implementation("org.apache.logging.log4j:log4j-core:2.20.0")

  // Testing (optional)
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
  testImplementation("dev.restate:sdk-testing:$restateVersion")
}

// Configure protoc plugin
protobuf {
  protoc { artifact = "com.google.protobuf:protoc:3.24.3" }

  plugins {
    // The gRPC Kotlin plugin depends on the gRPC generated code
    id("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.58.0" }
    id("restate") { artifact = "dev.restate:protoc-gen-restate:$restateVersion:all@jar" }
  }

  generateProtoTasks {
    all().forEach {
      it.plugins {
        id("grpc")
        id("restate") {
          option("kotlin")
        }
      }
      it.builtins {
        // The Kotlin codegen depends on the Java generated code
        java {}
        id("kotlin")
      }
    }
  }
}

// Configure test platform
tasks.withType<Test> {
  useJUnitPlatform()
}