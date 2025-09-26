package com.ssafy.a602.game.play.capture

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

class LandmarkerManager(
    context: Context,
    poseModelAsset: String = "models/pose_landmarker_lite.task",
    handModelAsset: String = "models/hand_landmarker.task"
) : AutoCloseable {
  val pose: PoseLandmarker
  val hand: HandLandmarker

    init {
        val poseBase = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath(poseModelAsset)
            .build()
        val handBase = com.google.mediapipe.tasks.core.BaseOptions.builder()
            .setModelAssetPath(handModelAsset)
            .build()
        pose = PoseLandmarker.createFromOptions(
            context,
            PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(poseBase)
                .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.VIDEO)
                .setNumPoses(1)
                .build()
        )
        hand = HandLandmarker.createFromOptions(
            context,
            HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(handBase)
                .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.VIDEO)
                .setNumHands(2)
                .build()
        )
    }

  data class LM(val x: Float, val y: Float, val z: Float = 0f, val v: Float = 0f)

  fun mapPose(r: com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult): List<LM> {
    val first = r.landmarks().firstOrNull() ?: return emptyList()
    return first.map { l -> LM(l.x(), l.y(), l.z(), l.visibility().orElse(0f)) }
  }

  fun mapHands(r: com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult): Pair<List<LM>, List<LM>> {
    val landmarks = r.landmarks()
    if (landmarks.isEmpty()) return emptyList<LM>() to emptyList()
    var left: List<LM> = emptyList()
    var right: List<LM> = emptyList()
    for (i in landmarks.indices) {
      val lm = landmarks[i].map { l -> LM(l.x(), l.y(), l.z(), 1f) }
      val label = try { r.handedness().get(i).first().categoryName() } catch (_: Exception) { null }
      if (label.equals("Left", true)) left = lm
      if (label.equals("Right", true)) right = lm
    }
    return left to right
  }

    override fun close() {
        try { 
            pose.close() 
        } catch (e: Exception) {
            android.util.Log.w("LandmarkerManager", "PoseLandmarker close 중 오류", e)
        }
        try { 
            hand.close() 
        } catch (e: Exception) {
            android.util.Log.w("LandmarkerManager", "HandLandmarker close 중 오류", e)
        }
    }
}
