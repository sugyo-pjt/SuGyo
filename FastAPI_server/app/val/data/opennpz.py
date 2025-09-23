import numpy as np

# npz 파일 열기
data = np.load('01-1__NIA_SL_WORD0001_REAL01_F.npz')

# 모든 배열 이름 출력
print(data.files)

# 배열별로 데이터 확인
for name in data.files:
    print(f"이름이 {name}:")
    print(data[name])
    print(f'{name}의 길이는{len(name)}')
    
data = np.load('01-1__NIA_SL_WORD0001_REAL01_F.npz', allow_pickle=True)
x = data['x']            # (L, 195)
orig_len = int(data['orig_len'])  # 실제 유효 프레임 길이
label = int(data['label'])

print('x.shape =', x.shape, 'orig_len =', orig_len, 'label =', label)

# 유효 구간만 보고 싶으면 패딩 제거
x_valid = x[:orig_len]

# 프린트 옵션: ellipsis 제거
np.set_printoptions(threshold=np.inf, linewidth=200, suppress=True)
print(x[:5])           # 또는 print(x)로 전체

