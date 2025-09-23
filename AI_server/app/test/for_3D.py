import numpy as np
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

def render_pose_3d(sequence: np.ndarray):
    """
    좌표 시퀀스 3D 렌더링
    sequence: (T, J, 3)  # T=프레임 수, J=관절 개수, 3=(x,y,z)
    """
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')

    for t in range(len(sequence)):
        ax.clear()
        xs, ys, zs = sequence[t, :, 0], sequence[t, :, 1], sequence[t, :, 2]
        ax.scatter(xs, ys, zs, c='r', marker='o')
        ax.set_xlim(-1, 1)
        ax.set_ylim(-1, 1)
        ax.set_zlim(-1, 1)
        plt.pause(0.05)  # 프레임 간격

    plt.show()

# 예시: 랜덤 좌표 20프레임, 관절 21개 (손)
dummy_seq = np.random.rand(20, 21, 3) * 2 - 1
render_pose_3d(dummy_seq)
