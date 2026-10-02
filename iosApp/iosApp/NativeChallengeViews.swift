import SwiftUI
import UIKit
import KMPObservableViewModelSwiftUI
import app

/// Rendering and input only. The retained owner initializes the shared session,
/// acknowledges result IDs, and navigates after an accepted shared outcome.
@MainActor
struct NativeChallengeView: View {
    @ObservedViewModel var model: AlarmMathViewModel
    let preview: Bool
    let retryInitialization: () -> Void
    var cancelPreview: (() -> Void)? = nil
    var returnFromStale: (() -> Void)? = nil
    var failure: AlarmErrorMessage? = nil
    var failureResultID: Int64? = nil
    var retryFailure: (() -> Void)? = nil
    var dismissFailure: (() -> Void)? = nil
    @FocusState private var answerFocused: Bool
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        Group {
            if model.state.readiness == .error {
                unavailable
            } else if model.state.readiness == .ready, let problem = model.state.currentProblem {
                challenge(problem, questionIndex: model.state.questionIndex)
            } else if model.state.readiness == .resolved {
                ContentUnavailableView(preview ? NativeStrings.text("Test complete") : NativeStrings.text("Alarm resolved"),
                    systemImage: "checkmark.circle")
                    .accessibilityIdentifier("challenge-resolved")
            } else {
                ProgressView("Preparing challenge…")
                    .accessibilityIdentifier("challenge-initializing")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .navigationTitle(preview ? NativeStrings.text("Test Alarm") : NativeStrings.text("Solve maths"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if preview, let cancelPreview {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) {
                        guard !model.isClosed else { return }
                        cancelPreview()
                    } label: {
                        Label("Cancel", systemImage: "xmark").labelStyle(.iconOnly)
                    }
                        .accessibilityLabel(Text("Cancel"))
                        .accessibilityIdentifier("challenge-cancel-preview")
                }
            }
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { answerFocused = false }
            }
        }
        .onAppear { focusAnswer() }
        .onChange(of: model.state.readiness) { _ in focusAnswer() }
        .onChange(of: model.state.finishing) { finishing in
            if !finishing { focusAnswer() }
        }
        .onChange(of: model.state.questionIndex) { _ in
            focusAnswer()
            if model.state.readiness == .ready {
                announce(NativeStrings.questionProgress(index: model.state.questionIndex, count: model.state.questionCount))
            }
        }
        .onChange(of: failure) { error in
            if failureResultID == nil, let error { announce(NativeStrings.error(error)) }
        }
        .onChange(of: failureResultID) { _ in
            if let error = failure { announce(NativeStrings.error(error)) }
        }
    }

    private var unavailable: some View {
        ContentUnavailableView {
            Label("Unable to load challenge", systemImage: "exclamationmark.triangle")
        } description: {
            Text(NativeStrings.error(model.state.error ?? .initialization))
        } actions: {
            if model.state.error == .staleOccurrence, let returnFromStale {
                Button("Return to alarms", action: returnFromStale)
            }
            Button("Try again", action: retryInitialization)
                .disabled(model.isClosed)
                .accessibilityIdentifier("challenge-retry-initialization")
        }
        .accessibilityIdentifier("challenge-initialization-error")
    }

    private func challenge(_ problem: MathProblem, questionIndex: Int32) -> some View {
        Form {
            if preview {
                Section {
                    Label("Maths preview", systemImage: "function")
                } footer: {
                    Text("Practise with this draft. Your alarm and delivery progress are unchanged.")
                }
            }
            if let alarm = model.state.alarm, !alarm.title.isEmpty {
                Section { Text(alarm.title).font(.headline) }
            }
            Section {
                VStack(alignment: .leading, spacing: 16) {
                    Text(NativeStrings.questionProgress(index: model.state.questionIndex, count: model.state.questionCount))
                        .font(.subheadline).foregroundStyle(.secondary)
                        .accessibilityIdentifier("challenge-progress-label")
                    ProgressView(value: Double(model.state.questionIndex), total: Double(max(1, model.state.questionCount)))
                        .accessibilityLabel(Text("Challenge progress"))
                        .accessibilityValue(NativeStrings.questionProgress(index: model.state.questionIndex, count: model.state.questionCount))
                    Text(NativeStrings.problem(problem))
                        .font(.largeTitle.monospacedDigit())
                        .frame(maxWidth: .infinity, alignment: .center)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.vertical, 8)
                        .accessibilityLabel(NativeStrings.accessibleProblem(problem))
                        .accessibilityIdentifier("challenge-problem")
                }
                .padding(.vertical, 8)
                TextField("Answer", text: Binding(get: { model.state.answerText }, set: { value in
                    guard canInteract else { return }
                    model.onEvent(event: MathScreenEvent.EnteredAnswer(value: value))
                }))
                .keyboardType(.numbersAndPunctuation)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .focused($answerFocused)
                .onSubmit { submit(problem, questionIndex: questionIndex) }
                .accessibilityLabel(Text("Answer"))
                .accessibilityHint(Text("Enter a whole number. Negative answers are allowed."))
                .accessibilityIdentifier("challenge-answer")
                .disabled(!canInteract)
                Group {
                    if dynamicTypeSize.isAccessibilitySize {
                        VStack(alignment: .leading, spacing: 16) {
                            submitButton(problem, questionIndex: questionIndex)
                            clearButton
                        }
                    } else {
                        HStack {
                            clearButton
                            Spacer()
                            submitButton(problem, questionIndex: questionIndex)
                        }
                    }
                }
                // Each row's control receives its own action, rather than Form's row-wide button action.
                .buttonStyle(.borderless)
            }
            if let failure {
                Section {
                    Label(NativeStrings.error(failure), systemImage: "exclamationmark.triangle")
                        .foregroundStyle(.red)
                        .fixedSize(horizontal: false, vertical: true)
                        .accessibilityIdentifier("challenge-feedback")
                    if let retryFailure {
                        Button("Try again", action: retryFailure)
                            .disabled(!canInteract)
                            .accessibilityIdentifier("challenge-retry-command")
                    }
                    if let dismissFailure {
                        Button("OK", action: dismissFailure)
                            .accessibilityIdentifier("challenge-acknowledge-feedback")
                    }
                }
            }
            if model.state.finishing {
                Section {
                    ProgressView("Updating alarm…")
                        .accessibilityIdentifier("challenge-finishing")
                }
            }
            if !preview, let alarm = model.state.alarm, alarm.canSnooze {
                Section {
                    Button {
                        guard canInteract else { return }
                        model.onEvent(event: MathScreenEvent.OnSnoozeClick(alarm: alarm.alarmId, preview: false))
                    } label: {
                        LabeledContent("Snooze", value: NativeStrings.minutes(count: alarm.snooze))
                    }
                    .disabled(!canInteract)
                    .accessibilityIdentifier("challenge-snooze")
                }
            }
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private var canInteract: Bool {
        !model.isClosed && model.state.readiness == .ready && !model.state.finishing && model.state.preview == preview
    }

    private var clearButton: some View {
        Button("Clear") {
            guard canInteract else { return }
            model.onEvent(event: MathScreenEvent.OnClearClick.shared)
            answerFocused = true
        }
        .disabled(!canInteract || model.state.answerText.isEmpty)
        .accessibilityIdentifier("challenge-clear")
    }

    private func submitButton(_ problem: MathProblem, questionIndex: Int32) -> some View {
        Button("Submit answer") { submit(problem, questionIndex: questionIndex) }
            .buttonStyle(.borderedProminent)
            .disabled(!canInteract)
            .accessibilityIdentifier("challenge-submit")
    }

    private func submit(_ problem: MathProblem, questionIndex: Int32) {
        guard canInteract else { return }
        model.submitAnswer(questionIndex: questionIndex, problem: problem)
    }

    private func focusAnswer() {
        answerFocused = canInteract && model.state.currentProblem != nil
    }

    private func announce(_ message: String) {
        UIAccessibility.post(notification: .announcement, argument: message)
    }
}
