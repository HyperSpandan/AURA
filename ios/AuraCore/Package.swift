// swift-tools-version:5.9
import PackageDescription

// The iOS app's logic that doesn't touch the screen or the speaker: the
// YouTube Music client, its page parsers, lyrics and the library on disk.
// Foundation only, so it builds and tests on Linux as well as on a Mac.
let package = Package(
    name: "AuraCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [
        .library(name: "AuraCore", targets: ["AuraCore"]),
    ],
    targets: [
        .target(name: "AuraCore"),
        .testTarget(
            name: "AuraCoreTests",
            dependencies: ["AuraCore"],
            resources: [.copy("Fixtures")]
        ),
    ]
)
