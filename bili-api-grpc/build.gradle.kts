
import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.google.protobuf)
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.okhttp)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.kotlin)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.truth)
}

sourceSets["main"].proto {
    srcDir("./proto")
    ProtobufConfiguration.excludeProtoFiles.forEach(::exclude)
}

// protoc 生成的 Java 源码内含中文注释（UTF-8），Windows 下 javac 默认按平台字符集（GBK）
// 解码会报“不可映射字符”，因此固定使用 UTF-8 编译（与 gradle.properties 的 file.encoding 一致）
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("java") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.72.0"
        }
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.72.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                named("java") {
                }
                create("kotlin") {
                }
            }
            it.plugins {
                create("grpc") {
                }
                create("grpckt") {
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
