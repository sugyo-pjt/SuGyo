import os
import cv2
import mediapipe as mp

# MediaPipe 초기화
mp_drawing = mp.solutions.drawing_utils
mp_holistic = mp.solutions.holistic

video_dir = r"C:\Users\SSAFY\Desktop\signvideo"
out_dir = r"C:\Users\SSAFY\Desktop\VGG_video"
os.makedirs(out_dir, exist_ok=True)

listing = [f for f in os.listdir(video_dir) if f.endswith(".mp4")]

for file in listing:
    file_path = os.path.join(video_dir, file)

    try:
        person_id, sign_id, _ = file.replace(".mp4", "").split("-")
    except ValueError:
        print(f"⚠️ 잘못된 파일명 형식: {file}")
        continue

    # 저장 경로: 사람별 / 수어별
    target_dir = os.path.join(out_dir, f"person_{person_id}", f"sign_{sign_id}", file.replace(".mp4", ""))
    os.makedirs(target_dir, exist_ok=True)

    cap = cv2.VideoCapture(file_path)
    fps = int(cap.get(cv2.CAP_PROP_FPS))
    print(fps)

    # 프레임 추출 간격 설정
    if fps <= 35:
        frame_step = 5
    else :
        frame_step = 10
    
    frameId = 0
    saved = 0
    with mp_holistic.Holistic(
        static_image_mode=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
    ) as holistic:

        while True:
            success, frame = cap.read()
            if not success:
                break

            if frameId % frame_step == 0:
                # BGR → RGB
                image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                results = holistic.process(image)

                # 원본 프레임 위에 랜드마크 그리기
                image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
                if results.pose_landmarks:
                    mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS)
                if results.left_hand_landmarks:
                    mp_drawing.draw_landmarks(image, results.left_hand_landmarks, mp_holistic.HAND_CONNECTIONS)
                if results.right_hand_landmarks:
                    mp_drawing.draw_landmarks(image, results.right_hand_landmarks, mp_holistic.HAND_CONNECTIONS)

                # 이미지 크기 통일
                image = cv2.resize(image, (720, 1280))

                # 저장
                filename = os.path.join(target_dir, f"frame_{saved+1}.png")
                cv2.imwrite(filename, image)
                saved += 1

            frameId += 1

    cap.release()
    print(f"✅ Done: {file}, saved {saved} frames → {target_dir}")
