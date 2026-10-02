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
            if AppDelegate.pendingDeliveryRestorationFinished { sessions.refreshPendingDelivery() }
        }
        .onReceive(NotificationCenter.default.publisher(for: .mathAlarmPendingDelivery)) { _ in
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
            sessions.deliveryPresented && !settingsPresented
        }, set: { sessions.deliveryPresented = $0 })) {
            NavigationStack { NativePendingDelivery(sessions: sessions) }
                .interactiveDismissDisabled()
        }
    }
}
