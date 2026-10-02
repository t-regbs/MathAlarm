import SwiftUI
import KMPObservableViewModelSwiftUI
import app

@MainActor
struct ContentView: View {
    var body: some View {
        #if DEBUG
        if SharedBridgeVerification.enabled {
            Color.clear.task { await SharedBridgeVerification.run() }
        } else {
            NativeApplicationRoot()
        }
        #else
        NativeApplicationRoot()
        #endif
    }

}

/// The window owns feature instances; navigation columns only borrow them.
@MainActor
struct NativeApplicationRoot: View {
    @StateViewModel private var list = SharedFeatures.shared.list()
    @StateObject private var sessions = NativeWindowSessions()
    @ObservedObject private var restoration = NativeDeliveryRestoration.shared
    @State private var compactColumn: NavigationSplitViewColumn = .sidebar
    @State private var settingsPresented = false

    var body: some View {
        ZStack {
            NativeSessionOwners(sessions: sessions)
            NavigationSplitView(preferredCompactColumn: $compactColumn) {
                NativeAlarmList(model: list, sessions: sessions, openEditor: { alarm in
                    sessions.openEditor(alarm: alarm)
                    compactColumn = .detail
                }, openSettings: { settingsPresented = true })
            } detail: {
                NativeEditorStack(sessions: sessions)
            }
        }
        .onChange(of: sessions.selectedEditorID) { id in
            compactColumn = id == nil ? .sidebar : .detail
        }
        .onAppear {
            // Each new window independently restores acknowledged occurrences.
            // A retained ready flag from another window is not a replacement for loading.
            AppDelegate.checkPendingAlarmKitDeeplink(restoreUnresolved: true)
        }
        .onReceive(NotificationCenter.default.publisher(for: .mathAlarmPendingDelivery)) { _ in
            if restoration.failure != nil { settingsPresented = false }
            guard restoration.ready else { return }
            settingsPresented = false
            sessions.refreshPendingDelivery()
        }
        .sheet(isPresented: $settingsPresented) {
            NavigationStack {
                NativeDevelopmentScreen(title: "Settings", milestone: 6)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { settingsPresented = false }
                        }
                    }
            }
        }
        .fullScreenCover(isPresented: Binding(get: {
            (sessions.deliveryPresented || restoration.failure != nil) && !settingsPresented
        }, set: { sessions.deliveryPresented = $0 }), onDismiss: { sessions.presentationEnded() }) {
            NavigationStack {
                if let failure = restoration.failure {
                    VStack(spacing: 24) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.largeTitle)
                            .accessibilityHidden(true)
                        Text(NativeStrings.error(failure))
                            .multilineTextAlignment(.center)
                            .accessibilityLabel(NativeStrings.error(failure))
                        Button("Retry") {
                            sessions.deliveryPresented = true
                            AppDelegate.checkPendingAlarmKitDeeplink(restoreUnresolved: true)
                        }
                        .buttonStyle(.borderedProminent)
                        .accessibilityIdentifier("deliveryRestorationRetry")
                    }
                    .padding()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .navigationTitle("Solve maths")
                } else if !restoration.ready {
                    ProgressView("Preparing challenge…")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .navigationTitle("Solve maths")
                } else {
                    NativePendingDelivery(sessions: sessions)
                }
            }
                .interactiveDismissDisabled()
        }
    }
}
