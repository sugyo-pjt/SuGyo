package com.ssafy.a602.game.play.dto

import com.ssafy.a602.game.play.input.FrameEntry
import com.ssafy.a602.game.play.input.LM

private fun LM.toCoordinate() = Coordinate(
    x = x ?: 0f, 
    y = y ?: 0f, 
    z = z ?: 0f, 
    w = w ?: 0f
)

fun List<FrameEntry>.toFrameBlocks(): List<FrameBlock> =
    map { fe ->
        FrameBlock(
            frame = fe.frameIndex + 1, // 1-base
            poses = listOf(
                PoseBlock(part = "BODY",       coordinates = fe.pose.map { it.toCoordinate() }),
                PoseBlock(part = "LEFT_HAND",  coordinates = fe.left.map { it.toCoordinate() }),
                PoseBlock(part = "RIGHT_HAND", coordinates = fe.right.map { it.toCoordinate() })
            )
        )
    }

fun buildAllFramesEnvelope(
    action: GameActionType,
    timestampMs: Long,
    entries: List<FrameEntry>
): AllFramesEnvelope =
    AllFramesEnvelope(
        allFrames = listOf(
            ActionFrames(
                action = action,
                timestamp = timestampMs,
                frames = entries.toFrameBlocks()
            )
        )
    )
