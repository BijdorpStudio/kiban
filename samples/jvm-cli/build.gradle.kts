plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    application
}

ktfmt { kotlinLangStyle() }

dependencies { implementation(project(":library")) }

application { mainClass.set("nl.bijdorpstudio.kiban.samples.jvmcli.MainKt") }
