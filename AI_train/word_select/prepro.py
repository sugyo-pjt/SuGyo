import pandas as pd
from pathlib import Path

# CSV 경로
src = r"C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_train\word_select\dataset_metadata.csv"

# word_id, word_gloss만 읽기
df = pd.read_csv(src, usecols=["word_id", "word_gloss"])

# 중복 제거 (word_gloss 기준)
df_unique = df.drop_duplicates(subset="word_gloss")

# 확인
print(f"원래 단어 개수: {len(df)}")
print(f"중복 제거 후 단어 개수: {len(df_unique)}")
print(df_unique.head())

# 저장
out_path = Path(src).with_name("word_id_gloss_unique.csv")
df_unique.to_csv(out_path, index=False, encoding="utf-8-sig")
print("저장 완료:", out_path)
