CREATE TABLE users
(
	id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    nickname VARCHAR(10) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(63) NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    self_introduction VARCHAR(300) 
);

CREATE TABLE term
(
    id			BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title		VARCHAR(255) NOT NULL,
    content		TEXT NOT NULL,
    mandatory	TINYINT NOT NULL,
    created_at	TIMESTAMP NOT NULL,
    updated_at	TIMESTAMP NOT NULL
);

CREATE TABLE user_agreement 
( 
    user_id     BIGINT NOT NULL,
    term_id		BIGINT NOT NULL,
    is_agreed	TINYINT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, term_id),
    CONSTRAINT fk_user_agreement_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_agreement_term FOREIGN KEY (term_id) REFERENCES term(id)
);

CREATE TABLE refresh_token 
(
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    issued_ip VARCHAR(50),
    issued_user_agent VARCHAR(255),
    expired_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE `music` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`title`	VARCHAR(255)	NOT NULL,
	`singer`	VARCHAR(50)	NOT NULL,
	`song_time`	TIME(3)	NOT NULL,
	`album_image_url`	VARCHAR(255)	NULL,
	`song_url`	VARCHAR(255)	NULL
);

CREATE TABLE `chart` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`music_id`	BIGINT	NOT NULL,
	`sequence`	INT	NOT NULL,
	`lyrics`	VARCHAR(255)	NOT NULL,
	`started_at`	TIME(3)	NOT NULL,
    CONSTRAINT FK_music_TO_chart FOREIGN KEY (music_id) REFERENCES music(id)
);

CREATE TABLE `chart_answer` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`chart_id`	BIGINT	NOT NULL,
	`started_at`	TIME(3)	NOT NULL,
	`ended_at`	TIME(3)	NOT NULL,
	`started_index`	TINYINT	NOT NULL,
	`ended_index`	TINYINT	NOT NULL,
	CONSTRAINT FK_chart_TO_chart_answer FOREIGN KEY (chart_id) REFERENCES chart(id)
);

CREATE TABLE `daily` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`day`	INT	NOT NULL,
	`total_count`	INT NOT	NULL,
	`sentence`	VARCHAR(255)	NULL
);

CREATE TABLE `vocabulary` (
                              `id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
                              `word`	VARCHAR(255)	NOT NULL,
                              `description`	TEXT	NOT NULL,
                              `video_url`	VARCHAR(255)	NOT NULL
);

CREATE TABLE `user_daily_vocabulary` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`user_id`	BIGINT	NOT NULL,
	`daily_id`	BIGINT	NOT NULL,
	`correct_count`	INT	NULL,
	CONSTRAINT FK_Users_TO_user_daily_vocabulary FOREIGN KEY (user_id) REFERENCES users(id),
	CONSTRAINT FK_daily_TO_user_daily_vocabulary FOREIGN KEY (daily_id) REFERENCES daily(id)
);

CREATE TABLE `daily_vocabulary` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`daily_id`	BIGINT	NOT NULL,
	`vocabulary_id`	BIGINT	NOT NULL,
	CONSTRAINT FK_daily_TO_daily_vocabulary FOREIGN KEY (daily_id) REFERENCES daily(id),
	CONSTRAINT FK_vocabulary_TO_daily_vocabulary FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id)
);

CREATE TABLE `game_result` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`music_id`	BIGINT	NOT NULL,
	`user_id`	BIGINT	NOT NULL,
	`score`	INT	NOT NULL,
	`created_at`	TIMESTAMP	NOT NULL,
	`updated_at`	TIMESTAMP	NOT NULL,
	CONSTRAINT FK_music_TO_rank FOREIGN KEY (music_id) REFERENCES music(id),
	CONSTRAINT FK_user_TO_rank FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE `frame_coordinates` (
	`id`	BIGINT	NOT NULL AUTO_INCREMENT PRIMARY KEY,
	`music_id`	BIGINT	NOT NULL,
	`time_passed`	DOUBLE	NOT NULL,
	`frame_data`	JSON	NOT NULL
);

INSERT INTO users (id,email,nickname,password,profile_image_url,created_at,updated_at,self_introduction) VALUES (
    1,
    'user@example.com',
    '그린Mate',
    '$argon2id$v=19$m=16384,t=2,p=1$hzZDOo+EBIxlm8qAmN1KBQ$GX7zN7fb/1rWKa6dPeLrnvVCCHbRScKH+0iqgfXHRXE',
    NULL,
    '2025-09-11 07:11:45',
    '2025-09-11 07:11:45',
    '안녕하세요! 제가 바로 길거리 청소왕입니다.'
);

INSERT INTO term (title, content, mandatory, created_at, updated_at)
VALUES
    ('서비스 이용약관', '서비스 이용에 관한 전반적인 내용을 담고 있습니다. 반드시 동의해야 합니다.', 1, NOW(), NOW()),
    ('개인정보 수집 및 이용 동의', '회원 관리를 위해 이메일, 닉네임 등의 개인정보를 수집합니다. 반드시 동의해야 합니다.', 1, NOW(), NOW()),
    ('마케팅 정보 수신 동의', '이벤트 및 프로모션 정보를 이메일 또는 SMS로 발송합니다. 동의하지 않아도 서비스 이용이 가능합니다.', 0, NOW(), NOW());
-- music 테이블에 삽입
INSERT INTO music (id, title, singer, song_time, album_image_url,song_url ) VALUES
(1, '징글벨', '김진환', '00:01:00', NULL,'jinglebell');
-- music 테이블 동일

-- chart 테이블 밀리초 단위
INSERT INTO chart (id, music_id, sequence, lyrics, started_at) VALUES
(1, 1, 1, '흰눈 사이로 썰매를 타고', '00:00:13.260'),
(2, 1, 2, '달리는 기분 상쾌도 하다', '00:00:17.240'),
(3, 1, 3, '종이 울려서 장단 맞추니', '00:00:21.140'),
(4, 1, 4, '흥겨워서 소리높여 노래 부르자', '00:00:25.000'),
(5, 1, 5, '종소리 울려라 종소리 울려', '00:00:28.960'),
(6, 1, 6, '우리 썰매 빨리 달려 종소리 울려라', '00:00:32.860'),
(7, 1, 7, '종소리 울려라 종소리 울려', '00:00:36.700'),
(8, 1, 8, '기쁜 노래 부르면서 빨리 달리자', '00:00:40.620');

-- chart_answer 테이블 밀리초 단위
INSERT INTO chart_answer (chart_id, started_at, ended_at, started_index, ended_index) VALUES
(1, '00:00:13.260', '00:00:14.260', 0, 1),
(2, '00:00:17.240', '00:00:18.240', 0, 2),
(3, '00:00:21.140', '00:00:24.730', 10, 12),
(4, '00:00:25.000', '00:00:28.160', 10, 11),
(5, '00:00:28.960', '00:00:31.160', 4, 6),
(6, '00:00:32.860', '00:00:34.470', 3, 4),
(7, '00:00:36.700', '00:00:38.750', 4, 6),
(8, '00:00:40.620', '00:00:42.570', 11, 12);

INSERT INTO `daily` (`day`, `total_count`, `sentence`) VALUES
(1, 6, 'This is sentence for day 1'),
(2, 10, 'This is sentence for day 2'),
(3, 10, 'This is sentence for day 3'),
(4, 10, 'This is sentence for day 4'),
(5, 10, 'This is sentence for day 5'),
(6, 10, 'This is sentence for day 6'),
(7, 10, 'This is sentence for day 7'),
(8, 10, 'This is sentence for day 8'),
(9, 10, 'This is sentence for day 9'),
(10, 10, 'This is sentence for day 10');

INSERT INTO vocabulary (word, description, video_url) VALUES
('안녕하세요,안녕하십니까,안녕히 가십시오,안녕히 계세요',
 '오른 손바닥으로 주먹을 쥔 왼 팔을 쓸어내린 다음, 두 주먹을 쥐고 바닥이 아래로 향하게하여 가슴 앞에서 아래로 내린다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20191004/624421/MOV000244910_700X466.mp4'),
('반갑다,반기다,재미,흥,흥취,희열,즐겁다,즐기다',
 '두 손을 약간 구부려 손끝을 양쪽 가슴에 대고 상하로 엇갈리게 두 번 움직인다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20191029/632420/MOV000235261_700X466.mp4'),
('만나다',
 '두 주먹의 1지를 펴서 마주 세웠다가 중앙으로 모아 마주 댄다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20191029/632284/MOV000252208_700X466.mp4'),
('당신',
 '오른손을 펴서 손바닥이 위로 손끝이 밖으로 향하게 하여 밖(상대방)으로 내민다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20200824/735063/MOV000251321_700X466.mp4'),
('너,네,자네',
 '오른 주먹의 1지를 펴서 끝이 밖으로 향하게 하여 약간 내민다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20191014/627265/MOV000251996_700X466.mp4'),
('당사자,본인',
 '오른 주먹의 1지를 펴서 끝이 아래로 향하게 하여 끝으로 명치 부위를 스쳐 올려 바닥이 안으로 향하게 세운 다음, 두 주먹의 4·5지를 펴서 끝이 위로 향하게 맞대고 세워 양옆으로 두 번 약간 돌리며 벌린다.',
 'http://sldict.korean.go.kr/multimedia/multimedia_files/convert/20240118/1261124/MOV000361375_700X466.mp4');

INSERT INTO daily_vocabulary (daily_id, vocabulary_id) VALUES
(1, 1),
(1, 2),
(1, 3),
(1, 4),
(1, 5),
(1, 6);

INSERT INTO user_daily_vocabulary (user_id, daily_id, correct_count) VALUES
(1, 1, 6),  -- day1
(1, 2, 10);  -- day2

INSERT INTO game_result (music_id,user_id,score,created_at,updated_at) VALUES
(1,1,1150,now(),now());

INSERT INTO music (id, title, singer, song_time, album_image_url,song_url ) VALUES
(2, 'Way Back Home', '몰라', '00:01:00', NULL,'none');
