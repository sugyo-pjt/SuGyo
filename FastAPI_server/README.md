# 기본 사용 방법

## 준오형이 봐야 하는거
1. app/api/route.py에서 api 연결과 들어가야 하는 데이터 형식 맞추기
<<<<<<< HEAD
2. requirementx.txt는 디렉토리 가장 상위에 있는걸로 해야함. 주석까지 달아서 정리 된 걸로 안하면 충돌남.
=======
2. app/models에 best_model.h5 가 우리가 학습시킨 모델
3. app/models에 label_encoder.pkl이 모델의 결과를 단어로 변환해주는 인코더
4. app/services/Dense.py가 분류하는 코드. 여기에 있는 dummy_sequence에 수어 좌표값이 들어가면 결과가 나올 것.(손 봐야함. 검증 필요)
5. app/services/for_3D.py가 좌표를 역으로 추출해주는 코드. 단어를 기준으로 좌표값 따서 3D 랜더링 하도록 할 것.(손 많이 봐야함)
>>>>>>> 2d648a75b41dddb4660f12be6da0e5f0902b05a7


## 임시 데이터 - 안봐도 됨. 나중에 수정하거나 날려도 되는 것.
1. services/run_model.py 