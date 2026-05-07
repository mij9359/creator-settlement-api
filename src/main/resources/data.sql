-- 크리에이터
INSERT INTO creator (id, name) VALUES
                                   ('creator-1', '김강사'),
                                   ('creator-2', '이강사'),
                                   ('creator-3', '박강사');

-- 강의
INSERT INTO course (id, creator_id, title) VALUES
                                               ('course-1', 'creator-1', 'Spring Boot 입문'),
                                               ('course-2', 'creator-1', 'JPA 실전'),
                                               ('course-3', 'creator-2', 'Kotlin 기초'),
                                               ('course-4', 'creator-3', 'MSA 설계');

-- 판매 내역 (과제 샘플 그대로)
INSERT INTO sale_record (id, course_id, student_id, amount, paid_at) VALUES
                                                                         ('sale-1', 'course-1', 'student-1',  50000, '2025-03-05 10:00:00'),
                                                                         ('sale-2', 'course-1', 'student-2',  50000, '2025-03-15 14:30:00'),
                                                                         ('sale-3', 'course-2', 'student-3',  80000, '2025-03-20 09:00:00'),
                                                                         ('sale-4', 'course-2', 'student-4',  80000, '2025-03-22 11:00:00'),
                                                                         ('sale-5', 'course-3', 'student-5',  60000, '2025-01-31 23:30:00'),
                                                                         ('sale-6', 'course-3', 'student-6',  60000, '2025-03-10 16:00:00'),
                                                                         ('sale-7', 'course-4', 'student-7', 120000, '2025-02-14 10:00:00');

-- 취소/환불 내역 (과제 시나리오 검증용)
-- creator-1의 2025-03 정산: 총 판매 260,000 - 환불 110,000 = 순 판매 150,000
INSERT INTO cancel_record (sale_record_id, refund_amount, cancelled_at, reason) VALUES
                                                                                    ('sale-3', 80000, '2025-03-25 10:00:00', '전액 환불'),  -- cancel-1
                                                                                    ('sale-4', 30000, '2025-03-23 11:00:00', '부분 환불'),  -- cancel-2
                                                                                    ('sale-5', 60000, '2025-02-02 09:00:00', '월 경계 환불'); -- cancel-3 (sale-5는 1월 판매, 취소는 2월)