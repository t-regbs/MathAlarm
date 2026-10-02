import UIKit
import SwiftUI
import app

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
struct ContentView: View {
    var body: some View {
        #if DEBUG
        if SharedBridgeVerification.enabled {
            Color.clear.task { await SharedBridgeVerification.run() }
        } else { composeContent }
        #else
        composeContent
        #endif
    }
    private var composeContent: some View {
        ComposeView().ignoresSafeArea(.keyboard).ignoresSafeArea(.all, edges: .bottom)
    }
}
