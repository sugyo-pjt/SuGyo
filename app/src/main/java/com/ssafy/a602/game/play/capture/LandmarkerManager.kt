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
) {
  val pose: PoseLandmarker
  val hand: HandLandmarker

  init {
    val poseBase = BaseOptions.builder().setModelAssetPath(poseModelAsset).build()
    val handBase = BaseOptions.builder().setModelAssetPath(handModelAsset).build()

    pose = PoseLandmarker.createFromOptions(
      context,
      PoseLandmarker.PoseLandmarkerOptions.builder()
        .setBaseOptions(poseBase)
        .setRunningMode(RunningMode.VIDEO) // ⭐️ VIDEO 모드
        .setNumPoses(1)
        .build()
    )
    hand = HandLandmarker.createFromOptions(
      context,
      HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(handBase)
        .setRunningMode(RunningMode.VIDEO) // ⭐️ VIDEO 모드
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

  fun close() { try { pose.close() } catch(_:Exception){}; try { hand.close() } catch(_:Exception){} }
}
