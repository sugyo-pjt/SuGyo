package com.ssafy.a602.game.play.dto

import com.ssafy.a602.game.play.input.FrameEntry
import com.ssafy.a602.game.play.input.LM

private fun LM.toCoordinate() = Coordinate(x, y, z, w)

fun List<FrameEntry>.toFrameBlocks(): List<FrameBlock> =
    map { fe ->
        FrameBlock(
            frame = fe.frameIndex + 1, // 1-base
            poses = listOf(
                PoseBlock(part = PosePart.BODY,       coordinates = fe.pose.map { it.toCoordinate() }),
                PoseBlock(part = PosePart.LEFT_HAND,  coordinates = fe.left.map { it.toCoordinate() }),
                PoseBlock(part = PosePart.RIGHT_HAND, coordinates = fe.right.map { it.toCoordinate() })
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
