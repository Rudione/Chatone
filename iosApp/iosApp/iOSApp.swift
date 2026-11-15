import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var systemBars = SystemBarsModel()

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .onOpenURL { LiveEventController.shared.open($0) }
                .onChange(of: scenePhase) { phase in
                    if phase == .active { LiveEventController.shared.refreshAvailability() }
                }
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
        NowPlayingController.shared.install()
        LiveEventController.shared.install()
        let controller = MainViewControllerKt.MainViewController(lowLatencyPlayers: IvsLowLatencyPlayerProvider())
        MessageContextMenus.shared.install(on: controller)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
