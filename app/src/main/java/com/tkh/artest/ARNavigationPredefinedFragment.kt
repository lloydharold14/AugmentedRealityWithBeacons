//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import com.google.ar.sceneform.math.Quaternion
//import com.google.ar.sceneform.math.Vector3
//import com.google.ar.sceneform.rendering.ModelRenderable
//import com.google.ar.sceneform.ux.ArFragment
//
//class ARNavigationPredefinedFragment : ArFragment() {
//
//    private var arrowRenderable: ModelRenderable? = null
//    private var arrowNode: Node? = null
//
//    private val waypoints = listOf(
//        Vector3(0f, 0f, -2f),   // 2 meters forward (shorter for testing)
//        Vector3(-2f, 0f, -2f),   // 2 meters left
//        Vector3(-2f, 0f, 0f)     // 2 meters forward
//    )
//    private var currentWaypointIndex = 0
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Load GLB model (place "arrow.glb" in assets)
//        ModelRenderable.builder()
//            .setSource(requireContext(), Uri.parse("arrow.glb"))
//            .build()
//            .thenAccept { modelRenderable ->
//                arrowRenderable = modelRenderable
//                addNavigationArrow()
//            }
//            .exceptionally { throwable ->
//                Log.e("ARNavigation", "Model loading failed", throwable)
//                null
//            }
//
//        arSceneView.scene.addOnUpdateListener { updateArrowPositionAndRotation() }
//    }
//
//    private fun addNavigationArrow() {
//        arrowNode = AnchorNode().apply {
//            renderable = arrowRenderable
//            setParent(arSceneView.scene)
//            worldPosition = Vector3(0f, 0f, -1f) // 1m in front
//        }
//    }
//
//    private fun updateArrowPositionAndRotation() {
//        val camera = arSceneView.scene.camera
//        val cameraPos = camera.worldPosition
//
//        if (currentWaypointIndex < waypoints.size) {
//            val target = waypoints[currentWaypointIndex]
//            val direction = Vector3.subtract(target, cameraPos).normalized()
//
//            arrowNode?.worldPosition = Vector3.add(cameraPos, direction.scaled(0.5f)) // 0.5m in front
//            arrowNode?.worldRotation = Quaternion.lookRotation(direction, Vector3.up())
//
//            if (Vector3.subtract(cameraPos, target).length() < 0.3f) {
//                currentWaypointIndex++
//            }
//        }
//    }
//}



package com.tkh.artest

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment

class ARNavigationPredefinedFragment : Fragment() {
    private var arFragment: CustomArFragment? = null
    private var arrowRenderable: ModelRenderable? = null
    private var arrowNode: Node? = null

    private val waypoints = listOf(
        Vector3(0f, 0f, -2f),
        Vector3(-2f, 0f, -2f),
        Vector3(-2f, 0f, 0f)
    )
    private var currentWaypointIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            id = View.generateViewId()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (val availability = ArCoreApk.getInstance().checkAvailability(requireContext())) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> setupAR()
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                ArCoreApk.getInstance().requestInstall(requireActivity(), true).let { installStatus ->
                    if (installStatus == ArCoreApk.InstallStatus.INSTALLED) {
                        setupAR()
                    } else {
                        showError("ARCore installation failed")
                    }
                }
            }
            else -> showError("AR not supported: ${availability.name}")
        }
    }

    private fun setupAR() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            initARScene()
        }
    }

    private fun initARScene() {
        arFragment = childFragmentManager.findFragmentById(view?.id ?: return) as? CustomArFragment
            ?: CustomArFragment().also {
                childFragmentManager.beginTransaction()
                    .replace(view?.id ?: return, it)
                    .commit()
            }

        ModelRenderable.builder()
            .setSource(requireContext(), R.raw.arrow)
            .setRegistryId("arrow_model")
            .build()
            .thenAccept { model ->
                arrowRenderable = model
                addNavigationArrow()
                setupUpdateListener()
            }
            .exceptionally { error ->
                showError("Failed to load model: ${error.message}")
                null
            }
    }

    private fun addNavigationArrow() {
        arrowNode = AnchorNode().apply {
            renderable = arrowRenderable
            setParent(arFragment?.arSceneView?.scene)
            worldPosition = Vector3(0f, 0f, -1f)
        }
    }

    private fun setupUpdateListener() {
        arFragment?.arSceneView?.scene?.addOnUpdateListener {
            updateArrowPositionAndRotation()
        }
    }

    private fun updateArrowPositionAndRotation() {
        val scene = arFragment?.arSceneView?.scene ?: return
        val camera = scene.camera
        val cameraPos = camera.worldPosition

        if (currentWaypointIndex < waypoints.size) {
            val target = waypoints[currentWaypointIndex]
            val direction = Vector3.subtract(target, cameraPos).normalized()

            arrowNode?.worldPosition = Vector3.add(cameraPos, direction.scaled(0.5f))
            arrowNode?.worldRotation = Quaternion.lookRotation(direction, Vector3.up())

            if (Vector3.subtract(cameraPos, target).length() < 0.3f) {
                currentWaypointIndex++
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initARScene()
        } else {
            showError("Camera permission required")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        Log.e("ARNavigation", message)
    }

    class CustomArFragment : ArFragment() {
        override fun getSessionConfiguration(session: Session?): Config {
            return Config(session).apply {
                depthMode = Config.DepthMode.AUTOMATIC
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
        fun newInstance() = ARNavigationPredefinedFragment()
    }
}
