// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "swift-console",
    platforms: [.macOS(.v12)],
    targets: [
        // Populated by CI/a local run before `swift build`: `./gradlew
        // :kiban:assembleKibanDebugXCFramework`, then copy
        // kiban/build/XCFrameworks/debug/Kiban.xcframework here. See ../README.md.
        .binaryTarget(name: "Kiban", path: "Frameworks/Kiban.xcframework"),
        .executableTarget(name: "SwiftConsole", dependencies: ["Kiban"]),
    ]
)
