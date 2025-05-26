// iosApp/iosApp/ARScanViewController.swift
import UIKit
import ARKit
import shared // Your Kotlin Multiplatform shared module

class ARScanViewController: UIViewController {

    // MARK: - UI Components

    // AR View
    private var arView: ARSCNView!

    // Progress and status
    private var scanProgressView: UIProgressView!
    private var instructionLabel: UILabel!
    private var surfaceCountLabel: UILabel!
    private var roomAreaLabel: UILabel!
    private var lightingIndicatorView: UIView!

    // Buttons
    private var scanButton: UIButton!
    private var finishButton: UIButton!

    // Container views
    private var statusContainerView: UIView!
    private var controlsContainerView: UIView!

    // MARK: - Kotlin Multiplatform Components

    private var arManager: ARManager!
    private var viewModel: ARScanViewModel!

    // State observation
    private var stateObserver: Closeable?

    // MARK: - Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup UI first
        setupUI()

        // Initialize AR components
        setupAR()

        // Observe state changes
        observeState()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        // Configure navigation bar
        navigationItem.title = "Scan Room"
        navigationController?.navigationBar.prefersLargeTitles = false
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        viewModel.pauseScanning()
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)

        // Clean up if we're being removed
        if isMovingFromParent {
            stateObserver?.close()
            viewModel.cleanup()
        }
    }

    // MARK: - Setup Methods

    private func setupUI() {
        view.backgroundColor = .systemBackground

        // Create AR View
        arView = ARSCNView()
        arView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(arView)

        // Create status container
        statusContainerView = UIView()
        statusContainerView.translatesAutoresizingMaskIntoConstraints = false
        statusContainerView.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.9)
        statusContainerView.layer.cornerRadius = 12
        view.addSubview(statusContainerView)

        // Create instruction label
        instructionLabel = UILabel()
        instructionLabel.translatesAutoresizingMaskIntoConstraints = false
        instructionLabel.textAlignment = .center
        instructionLabel.font = .systemFont(ofSize: 16, weight: .medium)
        instructionLabel.numberOfLines = 2
        statusContainerView.addSubview(instructionLabel)

        // Create progress view
        scanProgressView = UIProgressView(progressViewStyle: .default)
        scanProgressView.translatesAutoresizingMaskIntoConstraints = false
        scanProgressView.progressTintColor = .systemBlue
        statusContainerView.addSubview(scanProgressView)

        // Create info labels container
        let infoStackView = UIStackView()
        infoStackView.translatesAutoresizingMaskIntoConstraints = false
        infoStackView.axis = .horizontal
        infoStackView.distribution = .fillEqually
        infoStackView.spacing = 20
        statusContainerView.addSubview(infoStackView)

        // Surface count label
        surfaceCountLabel = UILabel()
        surfaceCountLabel.textAlignment = .center
        surfaceCountLabel.font = .systemFont(ofSize: 14)
        surfaceCountLabel.textColor = .secondaryLabel
        infoStackView.addArrangedSubview(surfaceCountLabel)

        // Room area label
        roomAreaLabel = UILabel()
        roomAreaLabel.textAlignment = .center
        roomAreaLabel.font = .systemFont(ofSize: 14)
        roomAreaLabel.textColor = .secondaryLabel
        infoStackView.addArrangedSubview(roomAreaLabel)

        // Create controls container
        controlsContainerView = UIView()
        controlsContainerView.translatesAutoresizingMaskIntoConstraints = false
        controlsContainerView.backgroundColor = UIColor.systemBackground.withAlphaComponent(0.9)
        view.addSubview(controlsContainerView)

        // Create buttons
        scanButton = createButton(title: "Start Scanning", color: .systemBlue)
        scanButton.addTarget(self, action: #selector(scanButtonTapped), for: .touchUpInside)
        controlsContainerView.addSubview(scanButton)

        finishButton = createButton(title: "Finish Scanning", color: .systemGreen)
        finishButton.addTarget(self, action: #selector(finishButtonTapped), for: .touchUpInside)
        finishButton.isEnabled = false
        finishButton.alpha = 0.5
        controlsContainerView.addSubview(finishButton)

        // Setup constraints
        setupConstraints()
    }

    private func createButton(title: String, color: UIColor) -> UIButton {
        let button = UIButton(type: .system)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.setTitle(title, for: .normal)
        button.backgroundColor = color
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .semibold)
        button.layer.cornerRadius = 8
        button.heightAnchor.constraint(equalToConstant: 50).isActive = true
        return button
    }

    private func setupConstraints() {
        NSLayoutConstraint.activate([
            // AR View
            arView.topAnchor.constraint(equalTo: view.topAnchor),
            arView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            arView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            arView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            // Status container
            statusContainerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            statusContainerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            statusContainerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),

            // Instruction label
            instructionLabel.topAnchor.constraint(equalTo: statusContainerView.topAnchor, constant: 16),
            instructionLabel.leadingAnchor.constraint(equalTo: statusContainerView.leadingAnchor, constant: 16),
            instructionLabel.trailingAnchor.constraint(equalTo: statusContainerView.trailingAnchor, constant: -16),

            // Progress view
            scanProgressView.topAnchor.constraint(equalTo: instructionLabel.bottomAnchor, constant: 12),
            scanProgressView.leadingAnchor.constraint(equalTo: statusContainerView.leadingAnchor, constant: 16),
            scanProgressView.trailingAnchor.constraint(equalTo: statusContainerView.trailingAnchor, constant: -16),
            scanProgressView.heightAnchor.constraint(equalToConstant: 4),

            // Info stack view
            infoStackView.topAnchor.constraint(equalTo: scanProgressView.bottomAnchor, constant: 12),
            infoStackView.leadingAnchor.constraint(equalTo: statusContainerView.leadingAnchor, constant: 16),
            infoStackView.trailingAnchor.constraint(equalTo: statusContainerView.trailingAnchor, constant: -16),
            infoStackView.bottomAnchor.constraint(equalTo: statusContainerView.bottomAnchor, constant: -16),

            // Controls container
            controlsContainerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            controlsContainerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            controlsContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            // Scan button
            scanButton.topAnchor.constraint(equalTo: controlsContainerView.topAnchor, constant: 20),
            scanButton.leadingAnchor.constraint(equalTo: controlsContainerView.leadingAnchor, constant: 20),
            scanButton.trailingAnchor.constraint(equalTo: controlsContainerView.trailingAnchor, constant: -20),

            // Finish button
            finishButton.topAnchor.constraint(equalTo: scanButton.bottomAnchor, constant: 12),
            finishButton.leadingAnchor.constraint(equalTo: controlsContainerView.leadingAnchor, constant: 20),
            finishButton.trailingAnchor.constraint(equalTo: controlsContainerView.trailingAnchor, constant: -20),
            finishButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20)
        ])
    }

    private func setupAR() {
        // Create AR manager
        arManager = ARManager()

        // Create view model
        viewModel = ARScanViewModel(arManager: arManager, scope: nil)

        // Configure AR view
        arView.delegate = self
        arView.showsStatistics = false
        arView.automaticallyUpdatesLighting = true

        // Add debug options in development
        #if DEBUG
        arView.debugOptions = [.showFeaturePoints]
        #endif
    }

    private func observeState() {
        // Observe ViewModel state using Kotlin Flow
        stateObserver = FlowUtilsKt.observe(viewModel.uiState) { [weak self] state in
            guard let self = self, let state = state else { return }

            DispatchQueue.main.async {
                self.updateUI(with: state)
            }
        }
    }

    private func updateUI(with state: ARScanUiState) {
        // Update scan progress
        scanProgressView.setProgress(state.scanProgress, animated: true)

        // Update instruction text
        instructionLabel.text = state.scanningState.getInstructions()

        // Update surface count
        surfaceCountLabel.text = "Surfaces: \(state.detectedSurfaces)"

        // Update room area
        let areaInSqFt = state.roomArea * 10.764 // Convert m² to ft²
        roomAreaLabel.text = String(format: "Area: %.1f sq ft", areaInSqFt)

        // Update button states
        switch state.scanningState {
        case .idle:
            scanButton.setTitle("Start Scanning", for: .normal)
            scanButton.isEnabled = state.isARAvailable
            finishButton.isEnabled = false
            finishButton.alpha = 0.5

        case .scanning:
            scanButton.setTitle("Pause", for: .normal)
            scanButton.isEnabled = true
            finishButton.isEnabled = state.canFinishScanning
            finishButton.alpha = state.canFinishScanning ? 1.0 : 0.5

        case .paused:
            scanButton.setTitle("Resume", for: .normal)
            scanButton.isEnabled = true
            finishButton.isEnabled = state.canFinishScanning
            finishButton.alpha = state.canFinishScanning ? 1.0 : 0.5

        case .processing:
            scanButton.isEnabled = false
            finishButton.isEnabled = false
            scanButton.alpha = 0.5
            finishButton.alpha = 0.5

        case .completed:
            handleScanComplete(room: state.scannedRoom)

        case .error:
            showError(message: state.errorMessage ?? "Unknown error")

        default:
            break
        }

        // Show lighting quality indicator
        updateLightingIndicator(quality: state.lightingQuality)

        // Show scanning tips
        if state.scanningState == .scanning {
            showScanningTip(
                progress: state.scanProgress,
                surfaces: Int32(state.detectedSurfaces)
            )
        }

        // Handle error messages
        if let error = state.errorMessage {
            showError(message: error)
        }
    }

    private func updateLightingIndicator(quality: LightingQuality) {
        // Remove existing indicator
        lightingIndicatorView?.removeFromSuperview()

        // Create new indicator
        lightingIndicatorView = UIView(frame: CGRect(x: 0, y: 0, width: 24, height: 24))
        lightingIndicatorView.layer.cornerRadius = 12

        let color: UIColor
        let title: String

        switch quality {
        case .good:
            color = .systemGreen
            title = "Good lighting"
        case .fair:
            color = .systemYellow
            title = "Fair lighting"
        case .poor:
            color = .systemRed
            title = "Poor lighting"
        default:
            color = .systemGray
            title = "Unknown lighting"
        }

        lightingIndicatorView.backgroundColor = color

        // Create container with label
        let containerView = UIView()
        containerView.addSubview(lightingIndicatorView)

        let label = UILabel()
        label.text = title
        label.font = .systemFont(ofSize: 12)
        label.textColor = .secondaryLabel
        containerView.addSubview(label)

        // Layout
        lightingIndicatorView.translatesAutoresizingMaskIntoConstraints = false
        label.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            lightingIndicatorView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            lightingIndicatorView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            lightingIndicatorView.widthAnchor.constraint(equalToConstant: 24),
            lightingIndicatorView.heightAnchor.constraint(equalToConstant: 24),

            label.leadingAnchor.constraint(equalTo: lightingIndicatorView.trailingAnchor, constant: 8),
            label.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            label.trailingAnchor.constraint(equalTo: containerView.trailingAnchor)
        ])

        // Add to navigation bar
        navigationItem.rightBarButtonItem = UIBarButtonItem(customView: containerView)
    }

    private var currentTipView: UIView?

    private func showScanningTip(progress: Float, surfaces: Int32) {
        // Remove existing tip if any
        currentTipView?.removeFromSuperview()

        let tip = ARScanViewModelKt.getScanningTip(progress: progress, detectedSurfaces: surfaces)

        // Create tip view
        let tipView = UIView()
        tipView.backgroundColor = UIColor.systemBlue.withAlphaComponent(0.9)
        tipView.layer.cornerRadius = 8
        tipView.translatesAutoresizingMaskIntoConstraints = false

        let tipLabel = UILabel()
        tipLabel.text = tip
        tipLabel.textAlignment = .center
        tipLabel.textColor = .white
        tipLabel.font = .systemFont(ofSize: 14)
        tipLabel.numberOfLines = 0
        tipLabel.translatesAutoresizingMaskIntoConstraints = false

        tipView.addSubview(tipLabel)
        view.addSubview(tipView)

        currentTipView = tipView

        // Constraints
        NSLayoutConstraint.activate([
            tipLabel.topAnchor.constraint(equalTo: tipView.topAnchor, constant: 8),
            tipLabel.leadingAnchor.constraint(equalTo: tipView.leadingAnchor, constant: 12),
            tipLabel.trailingAnchor.constraint(equalTo: tipView.trailingAnchor, constant: -12),
            tipLabel.bottomAnchor.constraint(equalTo: tipView.bottomAnchor, constant: -8),

            tipView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            tipView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            tipView.bottomAnchor.constraint(equalTo: controlsContainerView.topAnchor, constant: -20)
        ])

        // Animate in
        tipView.alpha = 0
        UIView.animate(withDuration: 0.3) {
            tipView.alpha = 1
        }

        // Auto-hide after 3 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in
            UIView.animate(withDuration: 0.3, animations: {
                tipView.alpha = 0
            }) { _ in
                tipView.removeFromSuperview()
                if self?.currentTipView == tipView {
                    self?.currentTipView = nil
                }
            }
        }
    }

    private func handleScanComplete(room: Room?) {
        guard let room = room else { return }

        // Show success message
        let alert = UIAlertController(
            title: "Scan Complete!",
            message: "Successfully scanned \(room.surfaces.count) surfaces with a total area of \(Int(room.totalWallArea * 10.764)) sq ft.",
            preferredStyle: .alert
        )

        alert.addAction(UIAlertAction(title: "Continue to Paint Estimation", style: .default) { [weak self] _ in
            self?.navigateToPaintEstimation(room: room)
        })

        present(alert, animated: true)
    }

    private func navigateToPaintEstimation(room: Room) {
        // Navigate to paint estimation screen
        // This would be your next view controller
        print("Navigate to paint estimation with room: \(room.id)")

        // Example navigation (adjust based on your app structure):
        /*
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        if let paintVC = storyboard.instantiateViewController(withIdentifier: "PaintEstimationViewController") as? PaintEstimationViewController {
            paintVC.room = room
            navigationController?.pushViewController(paintVC, animated: true)
        }
        */
    }

    private func showError(message: String) {
        let alert = UIAlertController(
            title: "Scanning Error",
            message: message,
            preferredStyle: .alert
        )

        alert.addAction(UIAlertAction(title: "OK", style: .default) { [weak self] _ in
            self?.viewModel.resetScan()
        })

        present(alert, animated: true)
    }

    // MARK: - Actions

    @IBAction func scanButtonTapped(_ sender: UIButton) {
        switch viewModel.uiState.value.scanningState {
        case .idle:
            viewModel.startScanning()
        case .scanning:
            viewModel.pauseScanning()
        case .paused:
            viewModel.resumeScanning()
        default:
            break
        }
    }

    @IBAction func finishButtonTapped(_ sender: UIButton) {
        viewModel.finishScanning()
    }

    deinit {
        stateObserver?.close()
    }
}

// MARK: - ARSCNViewDelegate

extension ARScanViewController: ARSCNViewDelegate {

    func renderer(_ renderer: SCNSceneRenderer, nodeFor anchor: ARAnchor) -> SCNNode? {
        guard let planeAnchor = anchor as? ARPlaneAnchor else { return nil }

        // Create visualization for detected planes
        let node = SCNNode()

        // Create plane geometry
        let geometry = SCNPlane(
            width: CGFloat(planeAnchor.extent.x),
            height: CGFloat(planeAnchor.extent.z)
        )

        // Style based on plane type
        let material = SCNMaterial()
        switch planeAnchor.alignment {
        case .horizontal:
            // Check if it's floor or ceiling based on classification
            if #available(iOS 12.0, *) {
                switch planeAnchor.classification {
                case .floor:
                    material.diffuse.contents = UIColor.blue.withAlphaComponent(0.3)
                case .ceiling:
                    material.diffuse.contents = UIColor.cyan.withAlphaComponent(0.3)
                default:
                    material.diffuse.contents = UIColor.gray.withAlphaComponent(0.3)
                }
            } else {
                material.diffuse.contents = UIColor.blue.withAlphaComponent(0.3)
            }
        case .vertical:
            material.diffuse.contents = UIColor.green.withAlphaComponent(0.3)
        @unknown default:
            material.diffuse.contents = UIColor.gray.withAlphaComponent(0.3)
        }

        geometry.materials = [material]

        // Create plane node
        let planeNode = SCNNode(geometry: geometry)
        planeNode.eulerAngles.x = -.pi / 2

        node.addChildNode(planeNode)

        // Add edge visualization
        addEdgeVisualization(to: node, for: planeAnchor)

        return node
    }

    func renderer(_ renderer: SCNSceneRenderer, didUpdate node: SCNNode, for anchor: ARAnchor) {
        guard let planeAnchor = anchor as? ARPlaneAnchor,
              let planeNode = node.childNodes.first,
              let geometry = planeNode.geometry as? SCNPlane else { return }

        // Update plane size
        geometry.width = CGFloat(planeAnchor.extent.x)
        geometry.height = CGFloat(planeAnchor.extent.z)

        // Update edge visualization
        node.childNodes.filter { $0.name == "edge" }.forEach { $0.removeFromParentNode() }
        addEdgeVisualization(to: node, for: planeAnchor)
    }

    private func addEdgeVisualization(to node: SCNNode, for planeAnchor: ARPlaneAnchor) {
        let edgeGeometry = SCNPlane(
            width: CGFloat(planeAnchor.extent.x),
            height: CGFloat(planeAnchor.extent.z)
        )

        let edgeMaterial = SCNMaterial()
        edgeMaterial.diffuse.contents = UIColor.white
        edgeMaterial.isDoubleSided = true
        edgeMaterial.fillMode = .lines

        edgeGeometry.materials = [edgeMaterial]

        let edgeNode = SCNNode(geometry: edgeGeometry)
        edgeNode.eulerAngles.x = -.pi / 2
        edgeNode.name = "edge"

        node.addChildNode(edgeNode)
    }
}