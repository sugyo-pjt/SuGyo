import os

out_dir = r"C:\Users\SSAFY\Desktop\VGG_video"

for root, dirs, files in os.walk(out_dir):
    for file in files:
        if file.endswith(".png") and file.startswith("frame_"):
            try:
                # frame_번호 추출
                num = int(file.replace("frame_", "").replace(".png", ""))
                if num >= 19:  # 19 이상이면 삭제
                    file_path = os.path.join(root, file)
                    os.remove(file_path)
                    print(f"🗑️ Deleted: {file_path}")
            except ValueError:
                continue

print("✅ 완료: frame_19 이상은 모두 삭제됨")
