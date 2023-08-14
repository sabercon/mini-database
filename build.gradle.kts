plugins {
    id("java")
}

group = "cn.sabercon"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_20
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
