import os
import cv2
import time
import mediapipe as mp
from dotenv import load_dotenv

load_dotenv()

mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils

COUNTDOWN_SEC = int(os.getenv("COUNTDOWN_SEC"))

# 웹캠 열기
cap = cv2.VideoCapture(0)

if not cap.isOpened():
    raise RuntimeError("카메라를 열 수 없음.")

# 시작 시간 기록
started = False
start_time = time.time()
frame_idx = 0

# 모델 준비 : 손
with mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=2,
    model_complexity=1,
    min_detection_confidence=0.6,
    min_tracking_confidence=0.6
) as hands:

    while True:
        ok, frame = cap.read()

        if not ok:
            continue

        # 좌우 반전
        frame = cv2.flip(frame, 1)
        copyed_frame = frame.copy()
        H, W = copyed_frame.shape[:2]

        # 경과 시간 계산
        elapsed = int(time.time() - start_time)

        # 카운트 다운
        if not started:
            if elapsed < COUNTDOWN_SEC:
                text = str(COUNTDOWN_SEC - elapsed)
                cv2.putText(copyed_frame, text, (100, 100), cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 255, 0), 5)

            # 3초 이상 지나면 "START" 표시
            else:
                cv2.putText(copyed_frame, "START", (100, 100), cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 0, 255), 5)
                started = True
                frame_idx = 0

        # START 이후만 추출
        if started:
            # BGR -> RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result_hand = hands.process(image)

            image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

            if result_hand.multi_hand_landmarks:
                for hand_landmarks in result_hand.multi_hand_landmarks:
                    mp_drawing.draw_landmarks(
                        copyed_frame, hand_landmarks, mp_hands.HAND_CONNECTIONS
                    )

            # 프레임 + 1
            frame_idx += 1

        cv2.imshow("SELF CAMERA", copyed_frame)

        if cv2.waitKey(1) == ord("q"):
            break

cap.release()