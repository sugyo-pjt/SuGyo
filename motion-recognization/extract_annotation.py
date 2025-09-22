# 좌표 추출
def extract_annotation_to_json(result_pose, result_hand):
    frame_data = {}

    pose_landmarks = []
    left_hand_landmarks = []
    right_hand_landmarks = []

    # pose landmarks
    if result_pose.pose_landmarks:
        for point_idx, point in enumerate(result_pose.pose_landmarks.landmark):
            pose_landmarks.append({"x": point.x, "y": point.y, "z": point.z, "w": float(point.visibility)})
        frame_data["pose"] = pose_landmarks

    # hand landmarks
    if result_hand.multi_hand_landmarks and result_hand.multi_handedness:
        for idx, landmark in enumerate(result_hand.multi_hand_landmarks):
            label = result_hand.multi_handedness[idx].classification[0].label    # Left / Right
            hand_points = []
            for point_idx, point in enumerate(landmark.landmark):
                hand_points.append({"x": point.x, "y": point.y, "z": point.z, "w": 0.0})

            if label == "Left":
                left_hand_landmarks = hand_points
            else:
                right_hand_landmarks = hand_points

    if left_hand_landmarks:
        frame_data["left"] = left_hand_landmarks
    else:
        frame_data["left"] = []
    
    if right_hand_landmarks:
        frame_data["right"] = right_hand_landmarks
    else:
        frame_data["right"] = []
    
    return frame_data