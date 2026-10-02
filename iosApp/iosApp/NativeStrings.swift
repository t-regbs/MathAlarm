import Foundation
import app

/// Native presentation only: shared semantic values remain authoritative.
/// The String Catalog carries the existing nine locale translations and plurals.
enum NativeStrings {
    static func text(_ source: String) -> String {
        String(localized: String.LocalizationValue(source))
    }

    static func number(_ value: Int) -> String { value.formatted(.number) }
    static func number(_ value: Int32) -> String { number(Int(value)) }

    static func questions(_ count: Int) -> String {
        String(localized: "\(count) questions")
    }
    static func questions(_ count: Int32) -> String { questions(Int(count)) }

    static func questionProgress(index: Int32, count: Int32) -> String {
        String(format: text("Question %@ of %@"), number(index + 1), number(count))
    }

    static func problem(_ problem: MathProblem) -> String {
        "\(number(problem.numOne)) \(operationSymbol(problem.operator_)) \(number(problem.numTwo)) = ?"
    }

    static func accessibleProblem(_ problem: MathProblem) -> String {
        let operation: String
        switch problem.operator_ {
        case .add: operation = text("%@ plus %@")
        case .subtract: operation = text("%@ minus %@")
        case .times: operation = text("%@ multiplied by %@")
        default: operation = text("%@ divided by %@")
        }
        return String(format: operation, number(problem.numOne), number(problem.numTwo))
    }

    private static func operationSymbol(_ operation: MathProblemOperator) -> String {
        switch operation {
        case .add: "+"
        case .subtract: "−"
        case .times: "×"
        default: "÷"
        }
    }

    static func minutes(count: Int) -> String {
        Measurement(value: Double(count), unit: UnitDuration.minutes)
            .formatted(.measurement(width: .abbreviated, usage: .asProvided,
                                    numberFormatStyle: .number.precision(.fractionLength(0))))
    }
    static func minutes(count: Int32) -> String { minutes(count: Int(count)) }

    static func difficulty(_ value: Int32) -> String {
        // Presentation labels correspond to the shared preset indices.
        switch value {
        case 0: return text("Easy")
        case 1: return text("Medium")
        case 2: return text("Hard")
        default: return text("Custom")
        }
    }

    static func validation(_ validation: AlarmEditorValidation) -> String {
        if validation == .invalidTime { return text("Choose a valid alarm time.") }
        if validation == .invalidDays { return text("Choose valid weekdays.") }
        if validation == .notInitialized { return text("The alarm draft is not ready. Try again.") }
        return ""
    }

    static func error(_ error: AlarmErrorMessage) -> String {
        if error == .save { return text("Couldn’t save alarm. Try again.") }
        if error == .dismiss {
            return text("Couldn’t dismiss alarm. Try again.")
        }
        if error == .staleOccurrence { return text("This alarm occurrence is no longer active.") }
        if error == .initialization { return text("Couldn’t prepare this challenge. Try again.") }
        if error == .recovery { return text("Alarm recovery needs attention. Try again.") }
        if error == .snooze { return text("Couldn’t snooze alarm. Try again.") }
        if error == .tone { return text("Tone Unavailable") }
        if error == .incorrectAnswer { return text("Incorrect answer. Try again.") }
        return text("Couldn’t update alarm. Try again.")
    }
}
