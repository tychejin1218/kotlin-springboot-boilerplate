-- 컨테이너 최초 실행 시 적용되는 초기 스키마 (Testcontainers 통합 테스트도 같은 파일을 쓴다).
-- 기능 패키지를 추가할 때 테이블 DDL 을 여기에 이어서 작성한다.
USE boilerplate_db;

-- todo : 할 일 목록
CREATE TABLE IF NOT EXISTS todo (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '할 일 PK',
  title      VARCHAR(200) NOT NULL                COMMENT '할 일 내용',
  completed  TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '완료 여부 (0: 미완료, 1: 완료)',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                             COMMENT '생성일시 (UTC)',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시 (UTC)',
  deleted_at DATETIME              DEFAULT NULL                                          COMMENT '삭제일시 (NULL: 정상, NOT NULL: 논리 삭제)',
  PRIMARY KEY (id)
) COMMENT = '할 일';
