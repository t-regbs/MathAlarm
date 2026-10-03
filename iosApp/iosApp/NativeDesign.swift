import SwiftUI

/// Identity belongs to the content layer. Navigation, sheets, pickers and
/// switches retain the system's Liquid Glass appearance and accessibility.
enum MatAlarmPalette {
    static let accent = Color("AccentColor")
    // Prominent controls use a deeper violet so their system white labels
    // remain legible. Text and symbols use the lighter dark-mode accent.
    static let action = Color("MatAction")
    static let canvas = Color("MatCanvas")
    static let wash = Color("MatWash")
    static let surface = Color("MatSurface")
}

extension View {
    func matAlarmContent() -> some View {
        scrollContentBackground(.hidden)
            .background(MatAlarmPalette.canvas)
            .tint(MatAlarmPalette.accent)
    }
}

/// A static clock with arithmetic hour marks: a small, reusable signature.
/// It is decorative; actual alarm times and maths have separate spoken labels.
struct MatAlarmDial: View {
    var hour: Int = 7
    var minute: Int = 0
    @Environment(\.colorSchemeContrast) private var contrast

    var body: some View {
        GeometryReader { geometry in
            let side = min(geometry.size.width, geometry.size.height)
            ZStack {
                Circle()
                    .fill(MatAlarmPalette.surface)
                    .overlay {
                        Circle().fill(LinearGradient(
                            colors: [MatAlarmPalette.wash.opacity(0.15), MatAlarmPalette.wash.opacity(0.5)],
                            startPoint: .topLeading, endPoint: .bottomTrailing))
                    }
                    .overlay {
                        Circle().strokeBorder(MatAlarmPalette.accent.opacity(contrast == .increased ? 0.38 : 0.16),
                                              lineWidth: 1)
                    }
                    .shadow(color: MatAlarmPalette.accent.opacity(contrast == .increased ? 0 : 0.06),
                            radius: side * 0.04, y: side * 0.02)
                HourIndices()
                    .stroke(MatAlarmPalette.accent.opacity(contrast == .increased ? 0.5 : 0.26),
                            style: StrokeStyle(lineWidth: side * 0.012, lineCap: .round))
                ForEach(0..<4) { index in
                    Text(verbatim: ["+", "×", "=", "−"][index])
                        .font(.system(size: side * 0.14, weight: .medium, design: .rounded))
                        .foregroundStyle(MatAlarmPalette.accent.opacity(contrast == .increased ? 1 : 0.75))
                        .position(x: side * (0.5 + 0.375 * sin(Double(index) * .pi / 2)),
                                  y: side * (0.5 - 0.375 * cos(Double(index) * .pi / 2)))
                }
                DialHand(angle: (Double(hour % 12) + Double(minute) / 60) * .pi / 6, length: 0.205)
                    .stroke(MatAlarmPalette.accent,
                            style: StrokeStyle(lineWidth: side * 0.035, lineCap: .round))
                DialHand(angle: Double(minute) * .pi / 30, length: 0.27)
                    .stroke(MatAlarmPalette.accent,
                            style: StrokeStyle(lineWidth: side * 0.023, lineCap: .round))
                Circle()
                    .fill(MatAlarmPalette.accent)
                    .frame(width: side * 0.06, height: side * 0.06)
                    .overlay {
                        Circle().fill(MatAlarmPalette.surface)
                            .frame(width: side * 0.022, height: side * 0.022)
                    }
            }
            .frame(width: side, height: side)
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityHidden(true)
        .allowsHitTesting(false)
    }

    /// Eight quiet hour indices; the four cardinal positions belong to the
    /// arithmetic signature. No minute scale is needed at this illustration size.
    private struct HourIndices: Shape {
        func path(in rect: CGRect) -> Path {
            let center = CGPoint(x: rect.midX, y: rect.midY)
            let side = min(rect.width, rect.height)
            return Path { path in
                for index in 0..<12 where index % 3 != 0 {
                    let angle = Double(index) * .pi / 6
                    path.move(to: CGPoint(x: center.x + sin(angle) * side * 0.405,
                                         y: center.y - cos(angle) * side * 0.405))
                    path.addLine(to: CGPoint(x: center.x + sin(angle) * side * 0.435,
                                            y: center.y - cos(angle) * side * 0.435))
                }
            }
        }
    }

    private struct DialHand: Shape {
        let angle: Double
        let length: CGFloat

        func path(in rect: CGRect) -> Path {
            let center = CGPoint(x: rect.midX, y: rect.midY)
            let side = min(rect.width, rect.height)
            return Path { path in
                path.move(to: center)
                path.addLine(to: CGPoint(x: center.x + sin(angle) * side * length,
                                         y: center.y - cos(angle) * side * length))
            }
        }
    }
}

struct MatAlarmTimeLabel: View {
    let hour: Int32
    let minute: Int32
    var enabled = true

    var body: some View {
        let time = NativeAlarmPresentation.clockTime(hour: hour, minute: minute)
        let digits = Text(verbatim: time.digits)
            .font(.system(.largeTitle, design: .rounded, weight: enabled ? .semibold : .regular))
            .monospacedDigit()
            .foregroundStyle(enabled ? Color.primary : Color.secondary)
        let period = Text(verbatim: time.period)
            .font(.subheadline.weight(.medium))
            .foregroundStyle(.secondary)
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: 6) { digits; period }
                .fixedSize(horizontal: true, vertical: true)
            VStack(alignment: .leading, spacing: 4) { digits; period }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: time.formatted))
    }
}

struct MatAlarmIdentityHeader: View {
    let title: String
    let subtitle: String
    var eyebrow: String? = nil
    var emphasizesTime = false
    var hour: Int = 7
    var minute: Int = 0
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        HStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                if let eyebrow {
                    Text(verbatim: eyebrow)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(MatAlarmPalette.accent)
                }
                Text(verbatim: title)
                    .font(.system(emphasizesTime ? .largeTitle : .title2, design: .rounded, weight: .semibold))
                    .monospacedDigit()
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityAddTraits(.isHeader)
                Text(verbatim: subtitle)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if !dynamicTypeSize.isAccessibilitySize {
                MatAlarmDial(hour: hour, minute: minute).frame(width: 80, height: 80)
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(MatAlarmPalette.wash, in: RoundedRectangle(cornerRadius: 24))
    }
}

struct MatAlarmFeatureIcon: View {
    let symbol: String
    @ScaledMetric(relativeTo: .body) private var size = 32
    var body: some View {
        Image(systemName: symbol)
            .font(.system(.body, weight: .medium))
            .foregroundStyle(MatAlarmPalette.accent)
            .frame(width: size, height: size)
            .background(MatAlarmPalette.wash, in: RoundedRectangle(cornerRadius: 10))
            .accessibilityHidden(true)
    }
}

struct MatAlarmFeatureLabel: View {
    let title: LocalizedStringKey
    let symbol: String
    var body: some View {
        HStack(spacing: 12) {
            MatAlarmFeatureIcon(symbol: symbol)
            Text(title).foregroundStyle(.primary)
        }
    }
}

/// The Android empty-state artwork, using the same two-image arrangement.
struct MatAlarmEmptyIllustration: View {
    private let scale: CGFloat = 0.75

    var body: some View {
        ZStack(alignment: .topLeading) {
            Image("EmptyAlarmSearch")
                .resizable().scaledToFit()
                .frame(width: 167 * scale, height: 228 * scale)
                .offset(x: 40 * scale)
            Image("EmptyAlarmSearch")
                .resizable().scaledToFit()
                .frame(width: 167 * scale, height: 228 * scale)
                .offset(y: 16 * scale)
        }
        .frame(width: 207 * scale, height: 244 * scale, alignment: .topLeading)
        .foregroundStyle(.secondary)
        .accessibilityHidden(true)
        .allowsHitTesting(false)
    }
}

struct MatAlarmEmptyState: View {
    var selection = false
    var addAlarm: (() -> Void)? = nil
    var body: some View {
        VStack(spacing: 20) {
            if !selection { MatAlarmEmptyIllustration() }
            VStack(spacing: 8) {
                Text(selection ? NativeStrings.text("Select an alarm") : NativeStrings.text("No alarms"))
                    .font(.system(.title2, design: .rounded, weight: .semibold))
                    .fixedSize(horizontal: false, vertical: true)
                if selection {
                    Text(NativeStrings.text("Choose an alarm or add a new one."))
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            if let addAlarm {
                Button(action: addAlarm) {
                    Text("Add alarm")
                        .frame(minWidth: 120)
                        .fixedSize(horizontal: false, vertical: true)
                }
                    .buttonStyle(.glassProminent)
                    .tint(MatAlarmPalette.action)
                    .controlSize(.large)
                    .accessibilityIdentifier("empty-add-alarm")
            }
        }
        .multilineTextAlignment(.center)
        .padding(28)
        .frame(maxWidth: .infinity)
        .tint(MatAlarmPalette.accent)
    }
}
