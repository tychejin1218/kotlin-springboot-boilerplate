-- 컨테이너 최초 실행 시 적용되는 초기 스키마 (Testcontainers 통합 테스트도 같은 파일을 쓴다).
-- 기존 테이블을 DROP 한 뒤 다시 만들므로 직접 실행하면 데이터가 모두 지워진다.
-- 기능 패키지를 추가할 때 테이블 DDL 을 여기에 이어서 작성한다.
USE boilerplate_db;

-- mysql 클라이언트 기본 문자셋(latin1)으로 실행되면 한글 시드 데이터가 깨지므로 명시한다
SET NAMES utf8mb4;

-- FK 의존 순서의 역순으로 삭제
DROP TABLE IF EXISTS todo;
DROP TABLE IF EXISTS member;

-- member : 회원
--   email 은 로그인 ID 라 UNIQUE, password 는 BCrypt 인코딩 값
CREATE TABLE member (
  id             BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '회원 PK',
  email          VARCHAR(200) NOT NULL                               COMMENT '이메일 (로그인 ID)',
  name           VARCHAR(100) NOT NULL                               COMMENT '이름',
  password       VARCHAR(200) NOT NULL                               COMMENT 'BCrypt 인코딩된 비밀번호',
  role           VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER'           COMMENT '역할 (ROLE_USER, ROLE_ADMIN)',
  email_verified TINYINT(1)   NOT NULL DEFAULT 0                     COMMENT '이메일 인증 여부 (0: 미인증, 1: 인증)',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '생성일시 (UTC)',
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시 (UTC)',
  deleted_at     DATETIME              DEFAULT NULL                  COMMENT '삭제일시 (NULL: 정상, NOT NULL: 논리 삭제)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_member_email (email)
) COMMENT = '회원';

-- todo : 할 일 목록 (member_id 로 소유자 식별)
CREATE TABLE todo (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '할 일 PK',
  title      VARCHAR(200) NOT NULL                COMMENT '할 일 내용',
  completed  TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '완료 여부 (0: 미완료, 1: 완료)',
  member_id  BIGINT       NOT NULL                COMMENT '소유 회원 FK',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '생성일시 (UTC)',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시 (UTC)',
  deleted_at DATETIME              DEFAULT NULL                                          COMMENT '삭제일시 (NULL: 정상, NOT NULL: 논리 삭제)',
  PRIMARY KEY (id),
  KEY ix_todo_member_id (member_id),
  CONSTRAINT fk_todo_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
) COMMENT = '할 일';

-- 시드 데이터 (개발 환경 전용). 비밀번호 원문: Test1234!
INSERT INTO member (email, name, password, role, email_verified) VALUES
  ('user1@example.com', '홍길동', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1),
  ('user2@example.com', '김철수', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1),
  ('admin@example.com', '관리자', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_ADMIN', 1),
  ('deleted@example.com', '탈퇴회원', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1);

UPDATE member SET deleted_at = '2026-01-01 00:00:00' WHERE email = 'deleted@example.com';

INSERT INTO todo (title, completed, member_id) VALUES
  ('Spring Boot 프로젝트 세팅', 1, 1),
  ('JWT 인증 구현',             1, 1),
  ('Soft Delete 적용',          0, 1),
  ('페이지네이션 구현',          0, 1),
  ('React 프로젝트 세팅',       1, 2),
  ('로그인 페이지 구현',         1, 2),
  ('Todo 목록 UI 구현',         0, 2);
