import UIKit
import ComposeApp

@MainActor
final class MessageContextMenus: NSObject, UIGestureRecognizerDelegate {
    static let shared = MessageContextMenus()
    private weak var host: UIView?
    private weak var presenter: UIViewController?
    private weak var menuController: MessageMenuController?
    private lazy var hold = UILongPressGestureRecognizer(target: self, action: #selector(held(_:)))

    func install(on controller: UIViewController) {
        let bridge = NativeMessageMenus.shared
        bridge.enabled = true
        presenter = controller
        host = controller.view
        hold.delegate = self
        hold.minimumPressDuration = 0.45
        hold.allowableMovement = 10
        hold.cancelsTouchesInView = true
        controller.view.addGestureRecognizer(hold)
        bridge.onAvailabilityChanged = { [weak self] available in self?.hold.isEnabled = available.boolValue }
        hold.isEnabled = bridge.isAvailable
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        guard menuController == nil, let host else { return false }
        return hit(at: touch.location(in: host)) != nil
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer,
                           shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        true
    }

    private func hit(at point: CGPoint) -> NativeMessageMenuHit? {
        guard let host, let window = host.window else { return nil }
        let position = host.convert(point, to: window)
        let scale = window.screen.scale
        return NativeMessageMenus.shared.menuAt(x: position.x * scale, y: position.y * scale)
    }

    @objc private func held(_ gesture: UILongPressGestureRecognizer) {
        guard gesture.state == .began, menuController == nil,
              let host, let window = host.window, let presenter,
              presenter.presentedViewController == nil,
              let hit = hit(at: gesture.location(in: host)) else { return }
        let scale = window.screen.scale
        let windowRect = CGRect(x: hit.left / scale, y: hit.top / scale,
                                width: hit.width / scale, height: hit.height / scale)
        let rect = host.convert(windowRect, from: window).intersection(host.bounds)
        guard !rect.isEmpty else { return }
        let snapshot = host.resizableSnapshotView(from: rect, afterScreenUpdates: false, withCapInsets: .zero)
        let controller = MessageMenuController(menu: hit.menu, snapshot: snapshot, sourceRect: windowRect,
                                               touch: host.convert(gesture.location(in: host), to: window))
        menuController = controller
        presenter.present(controller, animated: false)
    }
}

private final class MessageMenuController: UIViewController {
    private let menu: NativeMessageMenu
    private let snapshot: UIView?
    private let sourceRect: CGRect
    private let touch: CGPoint
    private let shade = UIButton(type: .custom)
    private let preview = UIView()
    private let actions = UIStackView()
    private var presented = false
    private var closing = false

    init(menu: NativeMessageMenu, snapshot: UIView?, sourceRect: CGRect, touch: CGPoint) {
        self.menu = menu
        self.snapshot = snapshot
        self.sourceRect = sourceRect
        self.touch = touch
        super.init(nibName: nil, bundle: nil)
        modalPresentationStyle = .overFullScreen
        modalPresentationCapturesStatusBarAppearance = false
    }

    required init?(coder: NSCoder) { fatalError() }

    override func loadView() {
        view = UIView()
        view.backgroundColor = .clear
        view.accessibilityViewIsModal = true
        shade.backgroundColor = UIColor.black.withAlphaComponent(0.08)
        shade.accessibilityLabel = NSLocalizedString("Dismiss context menu", comment: "")
        shade.addAction(UIAction { [weak self] _ in self?.close() }, for: .touchUpInside)
        view.addSubview(shade)

        preview.backgroundColor = .secondarySystemBackground
        preview.layer.cornerRadius = 9
        preview.layer.shadowColor = UIColor.black.cgColor
        preview.layer.shadowOpacity = 0.12
        preview.layer.shadowRadius = 12
        preview.layer.shadowOffset = CGSize(width: 0, height: 3)
        preview.accessibilityIdentifier = "chat_message_preview"
        preview.isAccessibilityElement = true
        preview.accessibilityLabel = "\(menu.title): \(menu.body)"
        view.addSubview(preview)
        let content = UIView()
        content.clipsToBounds = true
        content.layer.cornerRadius = 7
        content.tag = 1
        preview.addSubview(content)
        if let snapshot {
            content.addSubview(snapshot)
        } else {
            let text = UILabel()
            text.text = "\(menu.title): \(menu.body)"
            text.font = .preferredFont(forTextStyle: .body)
            text.numberOfLines = 8
            content.addSubview(text)
        }

        actions.axis = .vertical
        actions.backgroundColor = .secondarySystemBackground
        actions.layer.cornerRadius = 13
        actions.clipsToBounds = true
        actions.layer.shadowColor = UIColor.black.cgColor
        actions.layer.shadowOpacity = 0.10
        actions.layer.shadowRadius = 10
        view.addSubview(actions)
        for (index, action) in menu.actions.enumerated() {
            if index > 0 {
                let line = UIView()
                line.backgroundColor = .separator
                line.heightAnchor.constraint(equalToConstant: 0.5).isActive = true
                actions.addArrangedSubview(line)
            }
            var configuration = UIButton.Configuration.plain()
            configuration.title = action.title
            configuration.image = UIImage(systemName: action.symbol)
            configuration.imagePlacement = .trailing
            configuration.imagePadding = 14
            configuration.contentInsets = NSDirectionalEdgeInsets(top: 10, leading: 15, bottom: 10, trailing: 15)
            configuration.baseForegroundColor = action.destructive ? .systemRed : .label
            let button = UIButton(configuration: configuration)
            button.contentHorizontalAlignment = .leading
            button.heightAnchor.constraint(greaterThanOrEqualToConstant: 46).isActive = true
            button.addAction(UIAction { [weak self] _ in self?.close(performing: action) }, for: .touchUpInside)
            actions.addArrangedSubview(button)
        }
        view.accessibilityElements = [preview, actions, shade]
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        shade.frame = view.bounds
        let safe = view.bounds.inset(by: UIEdgeInsets(top: view.safeAreaInsets.top + 10, left: 6,
                                                    bottom: view.safeAreaInsets.bottom + 10, right: 6))
        let actionWidth = min(280, safe.width)
        let actionHeight = actions.systemLayoutSizeFitting(CGSize(width: actionWidth, height: 0),
                                                         withHorizontalFittingPriority: .required,
                                                         verticalFittingPriority: .fittingSizeLevel).height
        let scale = min(1.035, safe.width / max(sourceRect.width, 1))
        let width = min(sourceRect.width * scale + 4, safe.width)
        let availableHeight = max(44, safe.height - actionHeight - 14)
        let height = min(sourceRect.height * scale + 4, min(availableHeight, 260))
        var row = CGRect(x: min(max(sourceRect.midX - width / 2, safe.minX), safe.maxX - width),
                         y: sourceRect.midY - height / 2, width: width, height: height)
        row.origin.y = min(max(row.minY, safe.minY), safe.maxY - height)
        let gap: CGFloat = 8
        let below = safe.maxY - row.maxY - gap
        let above = row.minY - safe.minY - gap
        let menuBelow = below >= actionHeight || below >= above
        if menuBelow && below < actionHeight { row.origin.y = max(safe.minY, safe.maxY - actionHeight - gap - height) }
        if !menuBelow && above < actionHeight { row.origin.y = min(safe.maxY - height, safe.minY + actionHeight + gap) }
        preview.frame = row
        if let content = preview.viewWithTag(1) {
            content.frame = preview.bounds.insetBy(dx: 2, dy: 2)
            let contentHeight = content.bounds.width * sourceRect.height / max(sourceRect.width, 1)
            snapshot?.frame = CGRect(origin: .zero, size: CGSize(width: content.bounds.width, height: contentHeight))
            if snapshot == nil { content.subviews.first?.frame = content.bounds }
        }
        let actionX = min(max(touch.x - actionWidth / 2, safe.minX), safe.maxX - actionWidth)
        actions.frame = CGRect(x: actionX, y: menuBelow ? row.maxY + gap : row.minY - gap - actionHeight,
                               width: actionWidth, height: actionHeight)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard !presented else { return }
        presented = true
        UIImpactFeedbackGenerator(style: .soft).impactOccurred(intensity: 0.65)
        shade.alpha = 0
        preview.alpha = 0
        actions.alpha = 0
        if !UIAccessibility.isReduceMotionEnabled {
            preview.transform = CGAffineTransform(scaleX: 0.97, y: 0.97)
            actions.transform = CGAffineTransform(translationX: 0, y: 4)
        }
        UIView.animate(withDuration: UIAccessibility.isReduceMotionEnabled ? 0.10 : 0.18,
                       delay: 0, options: [.beginFromCurrentState, .curveEaseOut]) {
            self.shade.alpha = 1
            self.preview.alpha = 1
            self.actions.alpha = 1
            self.preview.transform = .identity
            self.actions.transform = .identity
        }
        UIAccessibility.post(notification: .screenChanged, argument: preview)
    }

    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
        close(animated: false)
        super.viewWillTransition(to: size, with: coordinator)
    }

    private func close(performing action: NativeMessageAction? = nil, animated: Bool = true) {
        guard !closing else { return }
        closing = true
        UIView.animate(withDuration: animated ? 0.12 : 0, animations: {
            self.shade.alpha = 0
            self.preview.alpha = 0
            self.actions.alpha = 0
        }) { _ in
            self.dismiss(animated: false) { action?.perform() }
        }
    }
}
