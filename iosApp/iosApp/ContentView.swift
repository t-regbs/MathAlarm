import UIKit
import SwiftUI
import KMPObservableViewModelSwiftUI
import app

@MainActor
struct ContentView: View {
    var body: some View {
        #if DEBUG
        if SharedBridgeVerification.enabled {
            Color.clear.task { SharedBridgeVerification.start() }
        } else if NativePresentationVerification.enabled {
            Color.clear.task {
                guard let window = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene })
                    .flatMap(\.windows).first(where: \.isKeyWindow), let parent = window.rootViewController else {
                    preconditionFailure("Presentation verification requires an attached app window")
                }
                NativePresentationVerification.start(window: window, parent: parent)
            }
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
    @StateViewModel private var settings = SharedFeatures.shared.settings()
    @StateObject private var announcements = NativeAnnouncementPresentation()
    @StateObject private var sessions = NativeWindowSessions()
    @ObservedObject private var restoration = NativeDeliveryRestoration.shared
    @State private var compactColumn: NavigationSplitViewColumn = .sidebar
    @State private var settingsPresented = false
    @State private var settingsBeforeDelivery = false

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
            offerAnnouncementsIfIdle()
        }
        .onReceive(NotificationCenter.default.publisher(for: .mathAlarmPendingDelivery)) { _ in
            if restoration.failure != nil { interruptSupplementaryPresentation() }
            guard restoration.ready else { return }
            sessions.refreshPendingDelivery()
        }
        .background(NativeWindowAppearance(theme: settings.state.theme)
            .frame(width: 0, height: 0).accessibilityHidden(true))
        .alert("Couldn’t update preferences. Try again.", isPresented: Binding(get: {
            !settings.state.failures.isEmpty && !settingsPresented && !announcements.presented
                && !sessions.deliveryPresented && restoration.failure == nil
        }, set: { _ in })) {
            Button("Retry") {
                let failures = settings.state.failures
                settings.refreshAnnouncements()
                for failure in failures { settings.acknowledgeFailure(id: failure.id) }
                offerAnnouncementsIfIdle()
            }
            Button("OK", role: .cancel) {
                if let failure = settings.state.failures.first { settings.acknowledgeFailure(id: failure.id) }
            }
        }

        .onChange(of: list.state.loading) { _ in offerAnnouncementsIfIdle() }
        .onChange(of: sessions.selectedEditorID) { _ in offerAnnouncementsIfIdle() }
        .onChange(of: sessions.deliveryPresented) { presented in
            if presented { interruptSupplementaryPresentation() }
        }
        .onChange(of: restoration.ready) { _ in offerAnnouncementsIfIdle() }
        .onChange(of: restoration.failure) { failure in
            if failure != nil { interruptSupplementaryPresentation() }
        }
        .sheet(isPresented: Binding(get: { settingsPresented || announcements.presented }, set: {
            if !$0 { settingsPresented = false; announcements.presented = false }
        })) {
            NavigationStack {
                if announcements.presented {
                    NativeWhatsNew(model: settings, presentation: announcements, onTryFeature: { id in
                        settingsPresented = false
                        let editor = sessions.openEditor(alarm: nil)
                        sessions.setEditorPath([id == "math-challenges-v1" ? .challenge : .snooze], id: editor.id)
                        compactColumn = .detail
                    })
                } else {
                    NativeAppSettings(model: settings, openWhatsNew: {
                        announcements.reopen(model: settings)
                    }, onDone: { settingsPresented = false })
                }
            }
            .alert("Couldn’t update preferences. Try again.", isPresented: Binding(get: {
                !settings.state.failures.isEmpty
            }, set: { _ in })) {
                Button("OK", role: .cancel) {
                    if let failure = settings.state.failures.first { settings.acknowledgeFailure(id: failure.id) }
                }
            }
        }
        .fullScreenCover(isPresented: Binding(get: {
            (sessions.deliveryPresented || restoration.failure != nil) && !settingsPresented && !announcements.presented
        }, set: { sessions.deliveryPresented = $0 }), onDismiss: {
            sessions.presentationEnded()
            resumeSupplementaryPresentationIfResolved()
        }) {
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

    private func interruptSupplementaryPresentation() {
        settingsBeforeDelivery = settingsBeforeDelivery || settingsPresented
        settingsPresented = false
        announcements.interrupt()
    }

    private func resumeSupplementaryPresentationIfResolved() {
        guard sessions.challenge == nil, sessions.pendingPayload == nil,
              PendingDeeplinkStore.shared.peekPendingDeeplink() == nil,
              restoration.failure == nil else { return }
        if settingsBeforeDelivery {
            settingsBeforeDelivery = false
            settingsPresented = true
        }
        if announcements.resumeIfInterrupted() { return }
        offerAnnouncementsIfIdle()
    }

    private func offerAnnouncementsIfIdle() {
        guard !list.state.loading, sessions.selectedEditorID == nil,
              !settingsPresented, !sessions.deliveryPresented,
              sessions.challenge == nil, sessions.pendingPayload == nil,
              PendingDeeplinkStore.shared.peekPendingDeeplink() == nil, restoration.ready,
              restoration.failure == nil else { return }
        announcements.offerAutomatically(model: settings)
    }
}
