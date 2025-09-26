import os
import cv2
import shutil

# 원본 비디오 디렉토리
video_dir = r"C:\Users\SSAFY\Desktop\signvideo"
out_dir = r"C:\Users\SSAFY\Desktop\signvideo_sorted"

os.makedirs(out_dir, exist_ok=True)

listing = [f for f in os.listdir(video_dir) if f.endswith(".mp4")]

for file in listing:
    file_path = os.path.join(video_dir, file)

    # --- 파일명에서 수어번호 추출 ---
    # 형식: 사람번호-수어번호-XX.mp4
    try:
        person_id, sign_id, _ = file.split("-")
        sign_id = int(sign_id)  # 수어 번호
    except ValueError:
        print(f"⚠️ 잘못된 파일명 형식: {file}")
        continue

    # --- 영상 길이 확인 ---
    video = cv2.VideoCapture(file_path)
    total_frames = int(video.get(cv2.CAP_PROP_FRAME_COUNT))
    video.release()

    if total_frames <= 0:
        print(f"⚠️ 프레임 수 확인 불가: {file}")
        continue

    # 30프레임/60프레임 구분
    if total_frames <= 140:   # 30프레임 근처
        frame_group = "30frames"
    else: # 60프레임 근처
        frame_group = "60frames"


    # --- 저장 디렉토리 생성 ---
    target_dir = os.path.join(out_dir, f"sign_{sign_id}", frame_group)
    os.makedirs(target_dir, exist_ok=True)

    # --- 복사 ---
    target_path = os.path.join(target_dir, file)
    shutil.copy(file_path, target_path)

    print(f"✅ {file} → {target_dir}")
