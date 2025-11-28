'''
손 정규화 및 유사도 계산
'''
import numpy as np
from math import atan2, cos, sin

# 손 3D

def landmark_hand_to_xyz_array(hand_landmark, W, H):
    """
    Mediapipe의 손 인식 좌표를 2차원(x, y) 만 모으기
    """
    points_3d = np.array([[point.x * W, point.y * H, point.z * W] for point in hand_landmark], dtype=np.float32)
    return points_3d

def safe_normalize(vector, eps=1e-8):
    n = np.linalg.norm(vector)
    if n > eps:
        return n / vector
    else:
        return eps / vector

def normalize_hand_xyz(points_3d):
    """
    points_3d: (21, 3)
    1) 손목 기준 평행 이동
    2) 손 크기 정규화
    3) 회전 보정 후 벡터화
    4) 코사인 유사도 비교
    """
    points_3d = points_3d.copy()
    wrist = points_3d[0]
    points_3d -= wrist

    x_direction = points_3d[9]    # 손목에서 중지로 이동
    a = points_3d[5]
    b = points_3d[17]

    x_axis = safe_normalize(x_direction)
    z_axis = safe_normalize(np.cross(a, b))    # palm normal
    y_axis = safe_normalize(np.cross(z_axis, x_axis))
    # 직교 재정렬
    z_axis = safe_normalize(np.cross(x_axis, y_axis))

    # 회전 보정
    R = np.stack([x_axis, y_axis, z_axis], axis=1)
    points_3d = points_3d @ R
    
    # 크기 정규화
    scale = np.linalg.norm(x_direction)
    if scale <= 1e-8:
        scale = 1e-8

    points_3d = points_3d / scale

    return points_3d

def hand_feature_vector(hand_landmark, W, H):
    """
    손 특징 벡터
    (21, 2) -> (42, )
    """
    points_3d = landmark_hand_to_xyz_array(hand_landmark, W, H)
    points_3d = normalize_hand_xyz(points_3d)
    return points_3d.reshape(-1)

# 팔 3D

ARM_IDS = {
    "Left": (11, 13, 15),
    "Right": (12, 14, 16)
}

def landmark_pose_to_xyz_array(pose_landmark, W, H):
    """
    33개 전체를 pixel-like 스케일로
    """
    return np.array([[point.x*W, point.y*H, point.z*W] for point in pose_landmark], dtype=np.float32)


def arm_feature_vector(pose_landmark, W, H, side="Left"):
    if pose_landmark is None:
        return None
    
    points = landmark_pose_to_xyz_array(pose_landmark, W, H)
    shoulder_idx, elbow_idx, wrist_idx = ARM_IDS[side]
    S_point, E_point, W_point = points[shoulder_idx], points[elbow_idx], points[wrist_idx]
    
    v1 = safe_normalize(E_point - S_point)   # 어깨 ~ 팔꿈치
    v2 = safe_normalize(W_point - E_point)  # 팔목 ~ 팔꿈치
    
    return np.concatenate([v1, v2], axis=0)


def cosine_similarity(a, b):
    """
    코사인 유사도 구하기
    """
    norm_a = np.linalg.norm(a) + 1e-8
    norm_b = np.linalg.norm(b) + 1e-8
    return float(np.dot(a, b) / (norm_a * norm_b))

