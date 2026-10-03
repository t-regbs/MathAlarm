#if DEBUG
import SwiftUI

/// Assertion/report transport shared by XCTest and the retained command-line harness.
/// Never compiled into Release; no scheduler, persistence or navigation is replaced here.
@MainActor
final class VerificationResults: ObservableObject {
    static let shared = VerificationResults()
    @Published private(set) var finished = false
    private(set) var groups: [String] = []
    private(set) var assertionCount = 0
    var assertion: ((Bool, String, StaticString, UInt) -> Void)?
    func reset() { finished = false; groups = []; assertionCount = 0 }
    func check(_ condition: Bool, _ message: String, file: StaticString, line: UInt) {
        assertionCount += 1
        assertion?(condition, message, file, line)
        precondition(condition, message, file: file, line: line)
    }
    func pass(_ group: String) { groups.append(group); print("BRIDGE PASS \(group)") }
    func finish() { finished = true; print("SHARED_BRIDGE_VERIFICATION_PASSED") }
}

@MainActor
func verificationCheck(_ condition: @autoclosure () -> Bool,
                       _ message: @autoclosure () -> String = "Production integration assertion",
                       file: StaticString = #filePath, line: UInt = #line) {
    VerificationResults.shared.check(condition(), message(), file: file, line: line)
}

/// UI tests read an in-process report only after all mounted assertions complete.
struct VerificationResultView: View {
    @ObservedObject private var results = VerificationResults.shared
    var body: some View {
        Color.clear.overlay {
            if results.finished {
                Text(verbatim: "Verification complete")
                    .accessibilityIdentifier("verification-complete")
                    .accessibilityValue(results.groups.joined(separator: "\n"))
            }
        }
    }
}
#endif
