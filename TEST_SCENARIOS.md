# 테스트 시나리오 가이드

> 즉시 조회되는 GET 요청은 URL만 명시하고, 등록/취소 등 본문이 필요하거나 순서대로 실행해야 하는 시나리오는 요청 본문(JSON)을 함께 기재했습니다.

---

## 시나리오 요약표

| 단계 | 분류 | API | 의도 |
|---|---|---|---|
| S1 | 검증 | `GET /api/creators` | 데이터 적재 확인 |
| S2 | 검증 | `GET /api/settlements/creators/creator-1?yearMonth=2025-03` | 명세 핵심 케이스 |
| S3 | 검증 | `GET /api/settlements/creators/creator-3?yearMonth=2025-03` | 빈 월 조회 |
| S4-1 | 검증 | `GET /api/settlements/creators/creator-2?yearMonth=2025-01` | 캐시 hit (PAID) |
| S4-2 | 검증 | `GET /api/settlements/creators/creator-2?yearMonth=2025-02` | 음수 정산 |
| S5 | 등록 | `POST /api/sales` | 새 판매 등록 |
| S5-1 | 검증 | `GET /api/settlements/creators/creator-1?yearMonth=2025-04` | 신규 반영 확인 |
| S6 | 등록 | `POST /api/cancels` | 부분 환불 등록 |
| S6-1 | 검증 | `GET /api/settlements/creators/creator-2?yearMonth=2025-04` | stale 캐시 확인 |
| S7 | 음성 | `POST /api/cancels` | 누적 초과 환불 거부 |
| S8 | 음성 | `POST /api/cancels` | 시점 역전 거부 |
| S9-1 ~ S9-5 | 음성 | `POST /api/sales`, `/api/cancels` | DTO Validation |
| S11-1 | 상태전환 | `PATCH /api/settlements/2/confirm` | PENDING → CONFIRMED |
| S11-2 | 상태전환 | `PATCH /api/settlements/2/pay` | CONFIRMED → PAID |
| S12-1 | 음성 | `PATCH /api/settlements/3/pay` | 확정 단계 건너뜀 거부 |
| S12-2 | 음성 | `PATCH /api/settlements/1/confirm` | PAID 재확정 거부 |
| S12-3 | 음성 | `PATCH /api/settlements/9999/confirm` | 존재하지 않는 id |
| S13 | 검증 | `GET /api/settlements?startDate=2025-03-01&endDate=2025-03-31` | 운영자 집계 |
| S14 | 음성 | `GET /api/settlements?startDate=2025-03-31&endDate=2025-03-01` | 기간 역전 거부 |
| S15 | 검증 | row count 확인 | 중복 방지 |
| S16 | 등록+검증 | 다중 부분 환불 누적 | 누적 합산 |
| S19 | 검증 | `GET /api/settlements/creators/creator-1?yearMonth=2025-03` | 다중 강의 합산 |
| S20 | 등록+검증 | 12,345원 판매 | 절사 검증 |
| S21 | 등록+검증 | 23:59:59.999 paidAt | 밀리초 경계 |
| S22 | 등록 | 미래 paidAt | 정책 노출 |
| S24 | 등록+검증 | 당일 판매·취소 | 같은 날 처리 |
| S25 | 검증 | `GET /api/settlements?startDate=2025-03-05&endDate=2025-03-05` | 일별 집계 |

---

## A. 기본 정산 조회 (즉시 조회)

URL만으로 검증 가능한 시나리오입니다.

### S1. 데이터 적재 확인
```
GET /api/creators
```

### S2. 명세 핵심 케이스
```
GET /api/settlements/creators/creator-1?yearMonth=2025-03
```
**기대**: `totalSales: 260000`, `totalRefunds: 110000`, `settlementAmount: 120000`

### S3. 빈 월 조회
```
GET /api/settlements/creators/creator-3?yearMonth=2025-03
```
**기대**: 빈 정산 데이터 정상 응답

### S4-1. 월 경계 환불 - 판매월 (캐시 hit, PAID)
```
GET /api/settlements/creators/creator-2?yearMonth=2025-01
```
**기대**: `totalSales: 60000`, `totalRefunds: 0`, `status: PAID` (스냅샷 유지)

### S4-2. 월 경계 환불 - 환불월 (음수 정산)
```
GET /api/settlements/creators/creator-2?yearMonth=2025-02
```
**기대**: `totalSales: 0`, `totalRefunds: 60000`, `netSales: -60000`, `settlementAmount: -48000`

---

## B. 신규 데이터 등록 시나리오

### S5. 새 판매 등록 → 정산 즉시 반영

**요청**: `POST /api/sales`
```json
{
  "id": "sale-test-100",
  "courseId": "course-1",
  "studentId": "student-99",
  "amount": 100000,
  "paidAt": "2025-04-15T10:00:00"
}
```

### S5-1. 신규 판매 정산 반영 확인
```
GET /api/settlements/creators/creator-1?yearMonth=2025-04
```
**기대**: `totalSales: 100000`, `settlementAmount: 80000`

### S6. 부분 환불 등록

**요청**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-6",
  "refundAmount": 30000,
  "cancelledAt": "2025-04-20T10:00:00",
  "reason": "부분환불"
}
```

### S6-1. 부분 환불 정산 재조회 (stale 캐시 검증)
```
GET /api/settlements/creators/creator-2?yearMonth=2025-04
```
**기대**: 환불액이 즉시 반영되어야 함

---

## C. 비즈니스 룰 거부 (음성 검증)

### S7. 누적 환불 초과 거부

`sale-1`의 원 결제액은 50,000원. 누적 환불이 이를 초과하는 80,000원 요청.

**요청**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-1",
  "refundAmount": 80000,
  "cancelledAt": "2025-04-25T10:00:00",
  "reason": "초과환불"
}
```
**기대**: 400 + 누적 환불 초과 메시지

### S8. 취소 일시 < 결제 일시 거부

S5에서 등록한 `sale-test-100`의 결제일(2025-04-15)보다 이른 취소일(2025-04-01) 요청.

**요청**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-test-100",
  "refundAmount": 1000,
  "cancelledAt": "2025-04-01T10:00:00",
  "reason": "결제전취소"
}
```
**기대**: 400 + 시점 역전 메시지

---

## D. DTO Validation 검증

### S9-1. amount = 0 거부
**요청**: `POST /api/sales`
```json
{
  "courseId": "course-1",
  "studentId": "student-200",
  "amount": 0,
  "paidAt": "2025-05-01T10:00:00"
}
```
**기대**: 400 + `fieldErrors.amount`

### S9-2. amount 음수 거부
**요청**: `POST /api/sales`
```json
{
  "courseId": "course-1",
  "studentId": "student-200",
  "amount": -100,
  "paidAt": "2025-05-01T10:00:00"
}
```
**기대**: 400 + `fieldErrors.amount`

### S9-3. amount 누락
**요청**: `POST /api/sales`
```json
{
  "courseId": "course-1",
  "studentId": "student-200",
  "paidAt": "2025-05-01T10:00:00"
}
```
**기대**: 400 + `fieldErrors.amount: "결제 금액은 필수입니다"`

### S9-4. refundAmount 누락
**요청**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-1",
  "cancelledAt": "2025-04-01T10:00:00",
  "reason": "테스트"
}
```
**기대**: 400 + `fieldErrors.refundAmount: "환불 금액은 필수입니다"`

### S9-5. saleRecordId 누락
**요청**: `POST /api/cancels`
```json
{
  "refundAmount": 10000,
  "cancelledAt": "2025-04-01T10:00:00"
}
```
**기대**: 400 + `fieldErrors.saleRecordId`

---

## E. 정산 상태 전환

> **사전 작업**: H2 콘솔에서 settlement id 확인
> ```sql
> SELECT id, creator_id, settlement_year_month, status FROM settlement;
> ```

### S11-1. 정산 확정 (PENDING → CONFIRMED)
```
PATCH /api/settlements/2/confirm
```
**기대**: 200, H2에서 `status = CONFIRMED`, `updated_at` 갱신 확인

### S11-2. 정산 지급 완료 (CONFIRMED → PAID)
```
PATCH /api/settlements/2/pay
```
**기대**: 200, H2에서 `status = PAID` 확인

---

## F. 상태 전환 음성 검증

> **사전 준비**: S11 실행 후 id=2는 PAID 상태. 새 PENDING 정산이 필요하면 S3 재실행 후 H2에서 id 확인 (보통 id=3).

### S12-1. PENDING을 바로 pay (확정 단계 건너뜀)
```
PATCH /api/settlements/3/pay
```
**기대**: 400
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "CONFIRMED 상태인 정산만 '지급 완료' 처리가 가능합니다. 현재 상태: PENDING"
}
```

### S12-2. PAID 정산을 다시 confirm
```
PATCH /api/settlements/1/confirm
```
**기대**: 400 + 이미 처리된 상태 메시지

### S12-3. 존재하지 않는 id
```
PATCH /api/settlements/9999/confirm
```
**기대**: 404 또는 적절한 에러 응답

---

## G. 운영자 집계 (LEFT JOIN 검증)

### S13. 운영자 기간 정산 집계
```
GET /api/settlements?startDate=2025-03-01&endDate=2025-03-31
```
**기대 응답 (요약)**:
```json
{
  "period": {"startDate": "2025-03-01", "endDate": "2025-03-31"},
  "feeRate": 0.20,
  "creators": [
    {"creatorId":"creator-1", "totalSales":260000, "totalRefunds":110000, "netSales":150000, "feeAmount":30000, "settlementAmount":120000, "saleCount":4, "cancelCount":2},
    {"creatorId":"creator-2", "totalSales":60000,  "totalRefunds":0,      "netSales":60000,  "feeAmount":12000, "settlementAmount":48000,  "saleCount":1, "cancelCount":0},
    {"creatorId":"creator-3", "totalSales":0,      "totalRefunds":0,      "netSales":0,      "feeAmount":0,     "settlementAmount":0,      "saleCount":0, "cancelCount":0}
  ],
  "summary": {
    "totalCreators": 3,
    "activeCreators": 2,
    "totalSales": 320000,
    "totalRefunds": 110000,
    "totalNetSales": 210000,
    "totalFeeAmount": 42000,
    "totalSettlementAmount": 168000
  }
}
```

**검증 포인트**:
- `creator-3`이 활동 0건임에도 결과에 포함 (LEFT JOIN 정상)
- `activeCreators: 2` 정확히 계산
- `summary.totalSettlementAmount` = 120,000 + 48,000 + 0 = 168,000

### S14. 잘못된 기간 거부
```
GET /api/settlements?startDate=2025-03-31&endDate=2025-03-01
```
**기대**: 400 + `"종료일은 시작일 이후여야 합니다."`

---

## H. 중복 정산 방지

### S15. UNIQUE 제약 + findSavedSettlement 검증

**S15-1. row 수 기록** (H2 콘솔)
```sql
SELECT COUNT(*) FROM settlement
 WHERE creator_id = 'creator-1' AND settlement_year_month = '2025-03';
```
S2를 1회라도 호출했다면 1.

**S15-2. 동일 요청 재호출**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-03
```

**S15-3. row 수 재확인**
```sql
SELECT COUNT(*) FROM settlement
 WHERE creator_id = 'creator-1' AND settlement_year_month = '2025-03';
```
**기대**: 여전히 1 (새 INSERT 없음). 2가 되면 `findSavedSettlement` 실패.

---

## I. 다중 부분 환불 누적

### S16. 한 판매에 3차 부분 환불 + 4차 초과 거부

**S16-1. 판매 등록**: `POST /api/sales`
```json
{
  "id": "sale-test-200",
  "courseId": "course-2",
  "studentId": "student-201",
  "amount": 80000,
  "paidAt": "2025-05-01T10:00:00"
}
```

**S16-2. 1차 환불 20,000**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-test-200",
  "refundAmount": 20000,
  "cancelledAt": "2025-05-05T10:00:00",
  "reason": "1차 부분환불"
}
```

**S16-3. 2차 환불 30,000**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-test-200",
  "refundAmount": 30000,
  "cancelledAt": "2025-05-10T10:00:00",
  "reason": "2차 부분환불"
}
```

**S16-4. 3차 환불 30,000 (합계 80,000, 통과)**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-test-200",
  "refundAmount": 30000,
  "cancelledAt": "2025-05-15T10:00:00",
  "reason": "3차 부분환불"
}
```
**기대**: 201 Created

**S16-5. 4차 환불 1원 (합계 80,001, 거부)**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-test-200",
  "refundAmount": 1,
  "cancelledAt": "2025-05-20T10:00:00",
  "reason": "초과 시도"
}
```
**기대**: 400 + 초과 메시지

**S16-6. 정산 확인**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-05
```
**기대**: `totalSales: 80000`, `totalRefunds: 80000`, `netSales: 0`, `settlementAmount: 0`, `cancelCount: 3`

---

## J. 데이터 정합성 검증

### S19. 다중 강의 보유 크리에이터 합산 (즉시 조회)
```
GET /api/settlements/creators/creator-1?yearMonth=2025-03
```
**의의**: `creator-1`이 `course-1`, `course-2` 두 강의를 보유. JOIN/GROUP BY가 크리에이터 단위로 정확히 묶이는지 확인.

**기대**:
- course-1 매출: sale-1(50,000) + sale-2(50,000) = 100,000
- course-2 매출: sale-3(80,000) + sale-4(80,000) = 160,000
- 합산: 260,000 (saleCount = 4)

### S20. 절사 검증 (BigDecimal RoundingMode.DOWN)

**S20-1. 12,345원 판매 등록**: `POST /api/sales`
```json
{
  "id": "sale-trunc-1",
  "courseId": "course-1",
  "studentId": "student-trunc",
  "amount": 12345,
  "paidAt": "2025-06-01T10:00:00"
}
```

**S20-2. 정산 조회**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-06
```
**계산**: 12,345 × 0.20 = 2,469.00 → DOWN 절사 → 2,469
**기대**: `feeAmount: 2469`, `settlementAmount: 9876`

### S21. 밀리초 단위 월 경계

**S21-1. 7월 말일 23:59:59.999 결제 등록**: `POST /api/sales`
```json
{
  "id": "sale-ms-edge",
  "courseId": "course-1",
  "studentId": "student-ms",
  "amount": 10000,
  "paidAt": "2025-07-31T23:59:59.999"
}
```

**S21-2. 7월 정산 조회**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-07
```
**기대**: 포함되어 `totalSales`에 10,000 가산

**S21-3. 8월 정산 조회**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-08
```
**기대**: 미포함 (8월 정산 0원)

### S22. 미래 결제 등록 (정책 노출)

**요청**: `POST /api/sales`
```json
{
  "id": "sale-future",
  "courseId": "course-1",
  "studentId": "student-future",
  "amount": 50000,
  "paidAt": "2099-12-31T10:00:00"
}
```
**의의**: 현재 통과되는지 확인. 미래 결제 차단 정책이 필요한지 평가자에게 노출.

### S24. 동일 월에 판매와 취소가 같은 날 발생

**S24-1. 판매 등록**: `POST /api/sales`
```json
{
  "id": "sale-sameday",
  "courseId": "course-1",
  "studentId": "student-sameday",
  "amount": 40000,
  "paidAt": "2025-09-15T09:00:00"
}
```

**S24-2. 같은 날 환불**: `POST /api/cancels`
```json
{
  "saleRecordId": "sale-sameday",
  "refundAmount": 40000,
  "cancelledAt": "2025-09-15T17:00:00",
  "reason": "당일 변심"
}
```

**S24-3. 정산 확인**
```
GET /api/settlements/creators/creator-1?yearMonth=2025-09
```
**기대**: `totalSales: 40000`, `totalRefunds: 40000`, `saleCount: 1`, `cancelCount: 1`, `settlementAmount: 0`

### S25. 운영자 집계 - 1일짜리 기간 (즉시 조회)
```
GET /api/settlements?startDate=2025-03-05&endDate=2025-03-05
```
**의의**: `startDate == endDate`인 1일 기간이 정상 처리되는지 (`startAt: 00:00:00`, `endAtExclusive: 다음날 00:00:00`).

**기대**:
- `creator-1`의 sale-1(2025-03-05 10:00:00, 50,000) 1건만 포함
- `creator-1`: `totalSales: 50000`, `settlementAmount: 40000`
- `creator-2`, `creator-3`: 0원
- `summary.activeCreators: 1`
