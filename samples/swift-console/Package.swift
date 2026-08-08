// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "swift-console",
    platforms: [.macOS(.v12)],
    products: [
        .executable(name: "swift-console", targets: ["SwiftConsole"]),
        .executable(name: "probe-result", targets: ["ProbeResult"]),
        .executable(name: "probe-exceptions", targets: ["ProbeExceptions"]),
        .executable(name: "probe-kind-describe", targets: ["ProbeKindDescribe"]),
        .executable(name: "probe-kind-switch", targets: ["ProbeKindSwitch"]),
        .executable(name: "probe-undeclared-throw", targets: ["ProbeUndeclaredThrow"]),
    ],
    targets: [
        // Populated by CI/a local run before `swift build`: `./gradlew
        // :library:assembleKibanDebugXCFramework`, then copy
        // library/build/XCFrameworks/debug/Kiban.xcframework here. See ../README.md.
        .binaryTarget(name: "Kiban", path: "Frameworks/Kiban.xcframework"),
        .executableTarget(name: "SwiftConsole", dependencies: ["Kiban"]),
        // #9's actual open questions (Result<Iban>, the IbanParseException hierarchy,
        // Malformed.Kind, undeclared-exception behavior) each get their own target instead of
        // sharing SwiftConsole's. `swift build` fails an entire target on any single compile
        // error, so one wrong guess about generated Swift API shape must not cost us the CI
        // output of the other probes.
        .executableTarget(name: "ProbeResult", dependencies: ["Kiban"]),
        .executableTarget(name: "ProbeExceptions", dependencies: ["Kiban"]),
        .executableTarget(name: "ProbeKindDescribe", dependencies: ["Kiban"]),
        .executableTarget(name: "ProbeKindSwitch", dependencies: ["Kiban"]),
        .executableTarget(name: "ProbeUndeclaredThrow", dependencies: ["Kiban"]),
    ]
)
