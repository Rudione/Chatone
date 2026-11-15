package io.rudione.chatone.presentation.system

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGSize
import platform.Foundation.NSTimeInterval
import platform.UIKit.UIModalPresentationStyle
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationCurve
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorContextProtocol
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal class VideoAwareHostController(
    private val content: UIViewController
) : UIViewController(nibName = null, bundle = null) {

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(content)
        content.view.setFrame(view.bounds)
        content.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(content.view)
        content.didMoveToParentViewController(this)
    }

    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (NativeVideoPresence.isOnScreen) {
            content.viewWillTransitionToSize(size, ImmediateTransition(withTransitionCoordinator))
        } else {
            super.viewWillTransitionToSize(size, withTransitionCoordinator)
        }
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = content.preferredStatusBarStyle()

    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation = content.preferredStatusBarUpdateAnimation()

    override fun prefersStatusBarHidden(): Boolean = content.prefersStatusBarHidden()
}

@OptIn(ExperimentalForeignApi::class)
private class ImmediateTransition(
    private val origin: UIViewControllerTransitionCoordinatorProtocol
) : NSObject(), UIViewControllerTransitionCoordinatorProtocol {

    override fun animateAlongsideTransition(
        animation: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
        completion: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?
    ): Boolean = origin.animateAlongsideTransition(animation, completion)

    override fun animateAlongsideTransitionInView(
        view: UIView?,
        animation: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?,
        completion: ((UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit)?
    ): Boolean = origin.animateAlongsideTransitionInView(view, animation, completion)

    override fun notifyWhenInteractionChangesUsingBlock(
        handler: (UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit
    ) = origin.notifyWhenInteractionChangesUsingBlock(handler)

    override fun notifyWhenInteractionEndsUsingBlock(
        handler: (UIViewControllerTransitionCoordinatorContextProtocol?) -> Unit
    ) = origin.notifyWhenInteractionEndsUsingBlock(handler)

    override fun isAnimated(): Boolean = false

    override fun transitionDuration(): NSTimeInterval = 0.0

    override fun presentationStyle(): UIModalPresentationStyle = origin.presentationStyle()

    override fun initiallyInteractive(): Boolean = origin.initiallyInteractive()

    override fun isInterruptible(): Boolean = origin.isInterruptible()

    override fun isInteractive(): Boolean = origin.isInteractive()

    override fun isCancelled(): Boolean = origin.isCancelled()

    override fun percentComplete(): CGFloat = origin.percentComplete()

    override fun completionVelocity(): CGFloat = origin.completionVelocity()

    override fun completionCurve(): UIViewAnimationCurve = origin.completionCurve()

    override fun viewControllerForKey(key: String?): UIViewController? = origin.viewControllerForKey(key)

    override fun viewForKey(key: String?): UIView? = origin.viewForKey(key)

    override fun containerView(): UIView = origin.containerView()

    override fun targetTransform(): CValue<CGAffineTransform> = origin.targetTransform()
}
