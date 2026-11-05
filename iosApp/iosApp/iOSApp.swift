import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @StateObject private var systemBars = SystemBarsModel()

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .statusBarHidden(systemBars.immersive)
                .persistentSystemOverlays(systemBars.immersive ? .hidden : .automatic)
        }
    }
}

final class SystemBarsModel: ObservableObject {
    @Published var immersive = false

    init() {
        IosSystemBars.shared.onImmersiveChanged = { [weak self] hidden in
            self?.immersive = hidden.boolValue
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
