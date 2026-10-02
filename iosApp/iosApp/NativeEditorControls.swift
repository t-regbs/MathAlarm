import SwiftUI
import KMPObservableViewModelSwiftUI
import app

/// Native controls bind to one retained Kotlin draft. They never own persistence or validation.
@MainActor
struct NativeEditorControls: View {
    @ObservedViewModel var model: AlarmSettingsViewModel

    var body: some View {
        Section("Alarm") {
            DatePicker("Time", selection: time, displayedComponents: .hourAndMinute)
                .accessibilityIdentifier("editor-time")
            TextField("Label", text: Binding(get: { model.state.alarmTitle }, set: {
                model.onEvent(event: AddEditAlarmEvent.EnteredTitle(value: $0))
            }))
            .textInputAutocapitalization(.sentences)
            .submitLabel(.done)
            .accessibilityIdentifier("editor-title")
            Toggle("Enabled", isOn: Binding(get: { model.state.isOn }, set: {
                model.onEvent(event: AddEditAlarmEvent.ToggleEnabled(value: $0))
            }))
            .accessibilityIdentifier("editor-enabled")
            NavigationLink(value: NativeEditorDestination.repeatSettings) {
                LabeledContent("Repeat", value: NativeEditorPresentation.weekdays(model.state))
            }
            .accessibilityIdentifier("editor-repeat")
        }
        Section {
            NavigationLink(value: NativeEditorDestination.challenge) {
                LabeledContent("Challenge settings", value: NativeEditorPresentation.difficulty(model.state.challenge))
            }
            .accessibilityIdentifier("editor-challenge")
            NavigationLink(value: NativeEditorDestination.snooze) {
                LabeledContent("Snooze", value: model.state.snoozeEnabled
                    ? NativeStrings.minutes(count: Int(model.state.snoozeMinutes))
                    : NSLocalizedString("Off", comment: "Snooze disabled"))
            }
            .accessibilityIdentifier("editor-snooze")
            NavigationLink("Sound library", value: NativeEditorDestination.sound)
                .accessibilityIdentifier("editor-sound")
        }
        Section {
            NavigationLink("Test Alarm", value: NativeEditorDestination.preview)
                .accessibilityIdentifier("editor-test-alarm")
        } footer: {
            Text("Test Alarm is scheduled for Milestone 5. Your draft is retained.")
        }
    }

    private var time: Binding<Date> {
        Binding(get: {
            // A fixed date avoids changing the stored wall-clock time at DST boundaries.
            Calendar.current.date(from: DateComponents(year: 2001, month: 1, day: 1,
                hour: Int(model.state.alarmTime.hour), minute: Int(model.state.alarmTime.minute))) ?? Date()
        }, set: { date in
            let components = Calendar.current.dateComponents([.hour, .minute], from: date)
            model.onEvent(event: AddEditAlarmEvent.ChangeTime(value: TimeState(
                hour: Int32(components.hour ?? 0), minute: Int32(components.minute ?? 0))))
        })
    }
}

@MainActor
struct NativeRepeatSettings: View {
    @ObservedViewModel var model: AlarmSettingsViewModel

    var body: some View {
        Form {
            Section {
                Toggle("Repeat weekly", isOn: Binding(get: { model.state.repeatWeekly }, set: {
                    model.onEvent(event: AddEditAlarmEvent.ToggleRepeat(value: $0))
                }))
                .accessibilityIdentifier("repeat-weekly")
            }
            Section("Weekdays") {
                ForEach(weekdayOrder, id: \.self) { index in
                    Toggle(Calendar.current.weekdaySymbols[index], isOn: Binding(get: {
                        Array(model.state.dayChooser)[safe: index] == "T"
                    }, set: { enabled in
                        var days = Array(model.state.dayChooser)
                        guard days.indices.contains(index) else { return }
                        days[index] = enabled ? "T" : "F"
                        model.onEvent(event: AddEditAlarmEvent.ToggleDayChooser(value: String(days)))
                    }))
                    .accessibilityIdentifier("repeat-weekday-\(index)")
                }
            }
            draftFooter
        }
        .navigationTitle("Repeat")
        .disabled(model.state.isSaving)
    }

    private var weekdayOrder: [Int] {
        let first = Calendar.current.firstWeekday - 1
        return (0..<7).map { (first + $0) % 7 }
    }
}

@MainActor
struct NativeChallengeSettings: View {
    @ObservedViewModel var model: AlarmSettingsViewModel

    var body: some View {
        Form {
            Section {
                if model.state.challenge.difficultyMix.isEmpty {
                    Picker("Difficulty", selection: Binding(get: { model.state.challenge.difficulty }, set: { value in
                        change(difficulty: value)
                    })) {
                        ForEach(0..<4) { level in
                            Text(NativeEditorPresentation.difficultyName(level)).tag(Int32(level))
                        }
                    }
                    .accessibilityIdentifier("challenge-difficulty")
                    Stepper(value: Binding(get: { Int(model.state.challenge.questionCount) }, set: {
                        change(questionCount: Int32($0))
                    }), in: 1...Int(MathChallenge.companion.MAX_QUESTIONS)) {
                        Text(NativeStrings.questions(model.state.challenge.questionCount))
                    }
                    .accessibilityIdentifier("challenge-question-count")
                }
                if model.state.challenge.difficulty != MathChallenge.companion.CUSTOM {
                    Toggle("Mix difficulties", isOn: Binding(get: {
                        !model.state.challenge.difficultyMix.isEmpty
                    }, set: { model.setChallengeMixing(enabled: $0) }))
                    .accessibilityIdentifier("challenge-mixing")
                }
            }
            if !model.state.challenge.difficultyMix.isEmpty {
                Section {
                    ForEach(0..<3) { level in
                        Stepper(value: Binding(get: {
                            model.state.challenge.mixedDifficulties.filter { $0.intValue == level }.count
                        }, set: { model.setMixedQuestionCount(difficulty: Int32(level), count: Int32($0)) }),
                            in: 0...Int(MathChallenge.companion.MAX_QUESTIONS)) {
                            HStack(spacing: 8) {
                                Text(NativeEditorPresentation.difficultyName(level))
                                Text(model.state.challenge.mixedDifficulties.filter { $0.intValue == level }.count.formatted())
                                    .foregroundStyle(.secondary)
                            }
                            .accessibilityElement(children: .combine)
                        }
                        .accessibilityIdentifier("challenge-mix-\(level)")
                    }
                    LabeledContent("Questions", value: Int(model.state.challenge.questionCount).formatted())
                } footer: {
                    Text("Questions are presented from easiest to hardest.")
                }
            }
            if model.state.challenge.difficulty == MathChallenge.companion.CUSTOM {
                Section("Operations") {
                    operation(symbol: "+", label: "Addition")
                    operation(symbol: "−", label: "Subtraction")
                    operation(symbol: "×", label: "Multiplication")
                    operation(symbol: "÷", label: "Division")
                }
                if model.state.challenge.operations.contains("+") || model.state.challenge.operations.contains("−") {
                    rangePicker("Addition and subtraction", ranges: MathChallenge.companion.ADDITION_RANGES,
                        value: model.state.challenge.additionRange) { change(additionRange: $0) }
                }
                if model.state.challenge.operations.contains("×") || model.state.challenge.operations.contains("÷") {
                    rangePicker("Multiplication and division", ranges: MathChallenge.companion.FACTOR_RANGES,
                        value: model.state.challenge.factorRange) { change(factorRange: $0) }
                }
            }
            draftFooter
        }
        .navigationTitle("Challenge settings")
        .disabled(model.state.isSaving)
    }

    private func operation(symbol: String, label: String) -> some View {
        Toggle(NSLocalizedString(label, comment: "Math operation"), isOn: Binding(get: {
            model.state.challenge.operations.contains(symbol)
        }, set: { enabled in
            model.setChallengeOperation(operation: symbol, enabled: enabled)
        }))
        .accessibilityIdentifier("challenge-operation-\(label.lowercased())")
    }

    private func rangePicker(_ title: String, ranges: [KotlinIntRange], value: Int32,
                             update: @escaping (Int32) -> Void) -> some View {
        Section {
            Picker(NSLocalizedString(title, comment: "Math operand range"),
                selection: Binding(get: { value }, set: update)) {
                ForEach(ranges.indices, id: \.self) { index in
                    Text("\(Int(ranges[index].first).formatted())–\(Int(ranges[index].last).formatted())")
                        .tag(Int32(index))
                }
            }
        }
    }

    /// Construct user-selected fields only; Kotlin owns all normalization and draft comparison.
    private func change(difficulty: Int32? = nil, questionCount: Int32? = nil, operations: String? = nil,
                        additionRange: Int32? = nil, factorRange: Int32? = nil) {
        let current = model.state.challenge
        model.onEvent(event: AddEditAlarmEvent.OnChallengeChange(value: MathChallenge(
            difficulty: difficulty ?? current.difficulty,
            questionCount: questionCount ?? current.questionCount,
            operations: operations ?? current.operations,
            additionRange: additionRange ?? current.additionRange,
            factorRange: factorRange ?? current.factorRange,
            difficultyMix: current.difficultyMix)))
    }
}

@MainActor
struct NativeSnoozeSettings: View {
    @ObservedViewModel var model: AlarmSettingsViewModel

    var body: some View {
        Form {
            Section {
                Toggle("Allow snooze", isOn: Binding(get: { model.state.snoozeEnabled }, set: {
                    model.onEvent(event: AddEditAlarmEvent.ToggleSnooze(value: $0))
                }))
                .accessibilityIdentifier("snooze-enabled")
            }
            if model.state.snoozeEnabled {
                Section {
                    Stepper(value: Binding(get: { Int(model.state.snoozeMinutes) }, set: {
                        model.onEvent(event: AddEditAlarmEvent.ChangeSnoozeDuration(minutes: Int32($0)))
                    }), in: 1...30) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Snooze interval")
                            Text(NativeStrings.minutes(count: Int(model.state.snoozeMinutes)))
                                .foregroundStyle(.secondary)
                        }
                        .accessibilityElement(children: .combine)
                    }
                    .accessibilityIdentifier("snooze-interval")
                    Picker("Maximum snoozes", selection: Binding(get: { model.state.maxSnoozes }, set: {
                        model.onEvent(event: AddEditAlarmEvent.ChangeMaxSnoozes(value: $0))
                    })) {
                        Text("Unlimited").tag(Int32(0))
                        ForEach([1, 2, 3, 5], id: \.self) { count in
                            Text(count.formatted()).tag(Int32(count))
                        }
                    }
                    .accessibilityIdentifier("snooze-maximum")
                }
            }
            draftFooter
        }
        .navigationTitle("Snooze")
        .disabled(model.state.isSaving)
    }
}

private var draftFooter: some View {
    Section {
        Text("Changes apply to this draft. Save the alarm to keep them.")
            .font(.footnote).foregroundStyle(.secondary)
    }
}

enum NativeEditorPresentation {
    static func difficultyName(_ difficulty: Int) -> String {
        let names = ["Easy", "Medium", "Hard", "Custom"]
        return NSLocalizedString(names[safe: difficulty] ?? "Custom", comment: "Challenge difficulty")
    }

    static func difficulty(_ challenge: MathChallenge) -> String {
        challenge.difficultyMix.isEmpty ? difficultyName(Int(challenge.difficulty))
            : NSLocalizedString("Mixed difficulty", comment: "Challenge difficulty summary")
    }

    static func weekdays(_ state: AlarmEditorState) -> String {
        let selected = Array(state.dayChooser).enumerated().compactMap { index, value in
            value == "T" && index < 7 ? Calendar.current.shortWeekdaySymbols[index] : nil
        }
        if selected.isEmpty { return NSLocalizedString("Once", comment: "Alarm recurrence") }
        if state.repeatWeekly && selected.count == 7 {
            return NSLocalizedString("Every day", comment: "Alarm recurrence")
        }
        return ListFormatter.localizedString(byJoining: selected)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}
