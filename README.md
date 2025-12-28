# 🚀 OASYS / BEST 마이그레이션 도구 실행 가이드

이 문서는 **OASYS 마이그레이션 도구**를 사용하여 **SQL 변환** 및 **메타데이터(파라미터) 검증**을 수행하는 작업자를 위한 **통합 명령어 가이드**입니다.

---

## 📂 0. 기본 환경 설정

- **작업 디렉토리**: `D:\run`
- **실행 파일(JAR)**: `oasys-migration-tool-0.1.0-SNAPSHOT-all.jar`
- **경로 기준(baseDir)**: 모든 상대경로는 `-Doasys.migration.baseDir=.` (현재 실행 폴더)를 기준으로 해석됩니다.
- **테스트 실행 옵션(--max)**
    - 특정 건수만 테스트: `--max=10`
    - 전체 실행: `--max` 옵션을 **제외**하거나 `--max=0` 또는 `--max=-1`

### ✅ 시스템 폴더 구조

```text
D:\run
├─ oasys-migration-tool-0.1.0-SNAPSHOT-all.jar
├─ systems
│  ├─ oasys
│  │  ├─ mapping (service_sql_xref.csv, column_mapping.xlsx)
│  │  └─ sqls (MyBatis mapper XML 파일들)
│  └─ best
│     ├─ mapping (service_sql_xref.csv, column_mapping.xlsx)
│     └─ sqls (MyBatis mapper XML 파일들)
└─ output (자동 생성되는 산출물 폴더)
```

---

## 🛠️ 1. OASYS 시스템 실행

### 1-1) ASIS_ALIAS_TO_TOBE 모드

**설명**: ASIS SQL 형식을 유지하면서 **SELECT 절 컬럼들만** TOBE 명칭의 **Alias**로 변환합니다.

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/oasys/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.alias.AliasSqlGenerateCli `
  --system=oasys `
  --mode=ASIS_ALIAS_TO_TOBE `
  --csv="systems/oasys/mapping/service_sql_xref.csv" `
  --mapping="systems/oasys/mapping/column_mapping.xlsx" `
  --out="output/oasys/asis-alias-to-tobe" `
  --result="output/oasys/asis-alias-to-tobe.xlsx"
```

---

### 1-2) TOBE_SQL 모드

**설명**: ASIS SQL을 완전히 **TOBE 표준 SQL 형식**으로 변환하여 생성합니다.

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/oasys/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.alias.AliasSqlGenerateCli `
  --system=oasys `
  --mode=TOBE_SQL `
  --csv="systems/oasys/mapping/service_sql_xref.csv" `
  --mapping="systems/oasys/mapping/column_mapping.xlsx" `
  --out="output/oasys/tobe-sql" `
  --result="output/oasys/tobe-sql.xlsx"
```

---

### 1-3) 메타 비교 리포트 (Verification)

**설명**: ASIS와 TOBE 간의 **변수(매개변수) 일치 여부**를 검증하고 **엑셀 리포트**를 생성합니다.

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/oasys/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.cli.MigrationVerifyCli `
  --csv="systems/oasys/mapping/service_sql_xref.csv" `
  --tobeDir="output/oasys/tobe-sql" `
  --result="output/oasys/meta-compare.xlsx"
```

---

## 🛠️ 2. BEST 시스템 실행

### 2-1) ASIS_ALIAS_TO_TOBE 모드

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/best/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.alias.AliasSqlGenerateCli `
  --system=best `
  --mode=ASIS_ALIAS_TO_TOBE `
  --csv="systems/best/mapping/service_sql_xref.csv" `
  --mapping="systems/best/mapping/column_mapping.xlsx" `
  --out="output/best/asis-alias-to-tobe" `
  --result="output/best/asis-alias-to-tobe.xlsx"
```

---

### 2-2) TOBE_SQL 모드

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/best/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.alias.AliasSqlGenerateCli `
  --system=best `
  --mode=TOBE_SQL `
  --csv="systems/best/mapping/service_sql_xref.csv" `
  --mapping="systems/best/mapping/column_mapping.xlsx" `
  --out="output/best/tobe-sql" `
  --result="output/best/tobe-sql.xlsx"
```

---

### 2-3) 메타 비교 리포트 (Verification)

```powershell
java "-Doasys.migration.baseDir=." "-Doasys.migration.sqlsDir=systems/best/sqls" `
  -cp ".\oasys-migration-tool-0.1.0-SNAPSHOT-all.jar" `
  oasys.migration.cli.MigrationVerifyCli `
  --csv="systems/best/mapping/service_sql_xref.csv" `
  --tobeDir="output/best/tobe-sql" `
  --result="output/best/meta-compare.xlsx"
```

---

## 📝 데이터 준비 주의사항 (CSV 가이드)

`service_sql_xref.csv` 파일 작성 시 아래 사항을 반드시 준수하세요.

- **필수 헤더 5개 포함**
- **파일 인코딩: UTF-8**

### ✅ 필수 헤더 정의

| 헤더 | 의미 |
|---|---|
| `service` | 서비스 클래스 명 |
| `service_method` | 실행 메서드 명 |
| `file` | Mapper XML 파일 경로 |
| `namespace` | Mapper의 `namespace` |
| `id` | SQL ID |

> ⚠️ 헤더 누락/오탈자/인코딩 불일치(UTF-8 아님) 시 도구 실행 중 매핑 실패 또는 리포트 누락이 발생할 수 있습니다.

---

## 📌 빠른 실행 순서 추천

1) `ASIS_ALIAS_TO_TOBE` (Alias 변환 결과 확인)
2) `TOBE_SQL` (완전 변환 SQL 생성)
3) `Verification` (ASIS vs TOBE 파라미터 검증)

---
