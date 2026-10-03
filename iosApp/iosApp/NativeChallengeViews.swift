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
    @State private var answerScrollTask: Task<Void, Never>?
    @State private var challengeContentHeight: CGFloat = 0
    @ScaledMetric(relativeTo: .largeTitle) private var equationSize = 48
    private let answerScrollTarget = "native-challenge-answer-row"
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
        .background(MatAlarmPalette.canvas)
        .tint(MatAlarmPalette.accent)
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
            if answerFocused {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { answerFocused = false }
                        .accessibilityHint(Text("Dismiss keyboard"))
                        .accessibilityIdentifier("challenge-keyboard-done")
                }
            }
        }
        .onAppear { focusAnswer() }
        .onDisappear {
            answerFocused = false
            answerScrollTask?.cancel()
            answerScrollTask = nil
        }
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
        GeometryReader { viewport in
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 24) {
                        if model.state.questionCount > 1 || model.state.alarm?.title.isEmpty == false {
                            challengeHeading
                        }
                        Text(NativeStrings.problem(problem))
                            .font(.system(size: equationSize, weight: .semibold, design: .rounded))
                            .monospacedDigit()
                            .multilineTextAlignment(.center)
                            .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 1)
                            .minimumScaleFactor(dynamicTypeSize.isAccessibilitySize ? 1 : 0.6)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity)
                            .accessibilityLabel(NativeStrings.accessibleProblem(problem))
                            .accessibilityIdentifier("challenge-problem")
                        VStack(spacing: 16) {
                            answerField(problem, questionIndex: questionIndex)
                            if let failure { feedback(failure) }
                            submitButton(problem, questionIndex: questionIndex)
                            if model.state.finishing {
                                ProgressView("Updating alarm…")
                                    .accessibilityIdentifier("challenge-finishing")
                            }
                            if !preview, let alarm = model.state.alarm, alarm.canSnooze {
                                Button {
                                    guard canInteract else { return }
                                    model.onEvent(event: MathScreenEvent.OnSnoozeClick(alarm: alarm.alarmId, preview: false))
                                } label: {
                                    HStack(spacing: 8) {
                                        Text("Snooze")
                                        Text(verbatim: NativeStrings.minutes(count: alarm.snooze))
                                            .foregroundStyle(Color.secondary)
                                    }
                                    .font(.subheadline)
                                    .frame(maxWidth: .infinity, minHeight: 44)
                                    .contentShape(Rectangle())
                                }
                                .buttonStyle(.plain)
                                .foregroundStyle(MatAlarmPalette.accent)
                                .disabled(!canInteract)
                                .accessibilityIdentifier("challenge-snooze")
                            }
                        }
                    }
                    .frame(maxWidth: 480)
                    .padding(.horizontal, 28)
                    .padding(.vertical, 28)
                    .frame(maxWidth: .infinity)
                    .onGeometryChange(for: CGFloat.self) { $0.size.height } action: {
                        challengeContentHeight = $0
                    }
                }
                // Native alignment centers short content; taller layouts scroll.
                .defaultScrollAnchor(.center, for: .alignment)
                .scrollBounceBehavior(.basedOnSize)
                .scrollIndicators(.hidden)
                .scrollDismissesKeyboard(.interactively)
                .onChange(of: answerFocused) { focused in
                    if focused { revealAnswer(using: proxy, viewportHeight: viewport.size.height) }
                    else { answerScrollTask?.cancel(); answerScrollTask = nil }
                }
                .onChange(of: viewport.size) { _ in revealAnswer(using: proxy, viewportHeight: viewport.size.height) }
                .onChange(of: challengeContentHeight) { _ in revealAnswer(using: proxy, viewportHeight: viewport.size.height) }
                .onChange(of: dynamicTypeSize) { _ in revealAnswer(using: proxy, viewportHeight: viewport.size.height) }
                .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillChangeFrameNotification)) { _ in
                    revealAnswer(using: proxy, viewportHeight: viewport.size.height)
                }
                .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidShowNotification)) { _ in
                    revealAnswer(using: proxy, viewportHeight: viewport.size.height)
                }
            }
        }
    }

    private var challengeHeading: some View {
        VStack(spacing: 8) {
            if let alarm = model.state.alarm, !alarm.title.isEmpty {
                Text(verbatim: alarm.title)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if model.state.questionCount > 1 {
                Text(NativeStrings.questionProgress(index: model.state.questionIndex, count: model.state.questionCount))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(MatAlarmPalette.accent)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityIdentifier("challenge-progress-label")
            }
        }
        .multilineTextAlignment(.center)
    }

    private func answerField(_ problem: MathProblem, questionIndex: Int32) -> some View {
        HStack(spacing: 0) {
            // Balance the trailing clear target without merging its accessibility
            // into the field. Both native controls keep independent actions.
            Color.clear.frame(width: 44, height: 44).accessibilityHidden(true)
            TextField("Answer", text: Binding(get: { model.state.answerText }, set: { value in
                guard canInteract else { return }
                model.onEvent(event: MathScreenEvent.EnteredAnswer(value: value))
            }))
            .keyboardType(.numbersAndPunctuation)
            .font(.system(.title, design: .rounded, weight: .medium))
            .monospacedDigit()
            .multilineTextAlignment(.center)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.go)
            .focused($answerFocused)
            .onSubmit { submit(problem, questionIndex: questionIndex) }
            .accessibilityLabel(Text("Answer"))
            .accessibilityHint(Text("Enter a whole number. Negative answers are allowed."))
            .accessibilityIdentifier("challenge-answer")
            .frame(maxWidth: .infinity)
            clearButton
        }
        .padding(.horizontal, 4)
        .padding(.vertical, 14)
        .frame(minHeight: 72)
        .background(MatAlarmPalette.wash, in: RoundedRectangle(cornerRadius: 24))
        .id(answerScrollTarget)
        .disabled(!canInteract)
    }

    private func feedback(_ failure: AlarmErrorMessage) -> some View {
        VStack(spacing: 8) {
            HStack(alignment: .center, spacing: 8) {
                Label {
                    Text(NativeStrings.error(failure)).foregroundStyle(Color.primary)
                } icon: {
                    Image(systemName: "exclamationmark.circle").foregroundStyle(Color.red)
                }
                .font(.subheadline)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityIdentifier("challenge-feedback")
                if let dismissFailure {
                    Button(action: dismissFailure) {
                        Image(systemName: "xmark")
                            .font(.caption.weight(.semibold))
                            .frame(width: 44, height: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.secondary)
                    .accessibilityLabel(Text("OK"))
                    .accessibilityIdentifier("challenge-acknowledge-feedback")
                }
            }
            if let retryFailure {
                Button("Try again", action: retryFailure)
                    .disabled(!canInteract)
                    .frame(minHeight: 44)
                    .accessibilityIdentifier("challenge-retry-command")
            }
        }
    }

    /// Focused native input stays inside the current viewport after software
    /// keyboard, window/orientation and text-size changes. This is presentation
    /// work only; disappearance cancels it without touching the shared owner.
    private func revealAnswer(using proxy: ScrollViewProxy, viewportHeight: CGFloat) {
        answerScrollTask?.cancel()
        guard answerFocused, canInteract, challengeContentHeight > viewportHeight else {
            answerScrollTask = nil
            return
        }
        answerScrollTask = Task { @MainActor in
            // The first pass follows layout; the second follows keyboard insets.
            // didShow/viewport events repeat this with the final native geometry.
            for delay in [80, 300] {
                do { try await Task.sleep(for: .milliseconds(delay)) }
                catch { return }
                guard !Task.isCancelled, answerFocused, canInteract else { return }
                var transaction = Transaction()
                transaction.disablesAnimations = true
                withTransaction(transaction) {
                    proxy.scrollTo(answerScrollTarget, anchor: .center)
                }
            }
            answerScrollTask = nil
        }
    }

    private var canInteract: Bool {
        !model.isClosed && model.state.readiness == .ready && !model.state.finishing && model.state.preview == preview
    }

    private var clearButton: some View {
        Button {
            guard canInteract else { return }
            model.onEvent(event: MathScreenEvent.OnClearClick.shared)
            answerFocused = true
        } label: {
            Image(systemName: "xmark.circle.fill")
                .font(.body)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(.secondary)
        .opacity(model.state.answerText.isEmpty ? 0 : 1)
        .disabled(!canInteract || model.state.answerText.isEmpty)
        .accessibilityHidden(model.state.answerText.isEmpty)
        .accessibilityLabel(Text("Clear"))
        .accessibilityIdentifier("challenge-clear")
    }

    private func submitButton(_ problem: MathProblem, questionIndex: Int32) -> some View {
        Button { submit(problem, questionIndex: questionIndex) } label: {
            Text("Submit answer")
                .font(.headline)
                .frame(maxWidth: .infinity, minHeight: 28)
                .fixedSize(horizontal: false, vertical: true)
        }
        .buttonStyle(.glassProminent)
        .tint(MatAlarmPalette.action)
        .controlSize(.large)
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
        NativeAccessibility.announce(message)
    }
}
