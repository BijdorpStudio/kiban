// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "swift-console",
    platforms: [.macOS(.v12)],
    products: [
        .executable(name: "swift-console", targets: ["SwiftConsole"])
    ],
    targets: [
        // Populated by CI/a local run before `swift build`: `./gradlew
        // :library:assembleKibanDebugXCFramework`, then copy
        // library/build/XCFrameworks/debug/Kiban.xcframework here. See ../README.md.
        .binaryTarget(name: "Kiban", path: "Frameworks/Kiban.xcframework"),
        .executableTarget(name: "SwiftConsole", dependencies: ["Kiban"]),
    ]
)
