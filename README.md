C:\oasys-migration-tool 에서 실행


tobe 모드 명령어
.\gradlew clean runAliasSqlGenerateCli -PappArgs="--mode=ASIS_ALIAS_TO_TOBE --csv=mapping/service_sql_xref.csv --mapping=mapping/column_mapping.xlsx --out=output/asis-alias-sql --result=output/asis-alias-sql.xlsx --logEvery=100 --slowMs=500"


asis 모드 명령어
.\gradlew clean runAliasSqlGenerateCli -PappArgs="--mode=TOBE_SQL --csv=mapping/service_sql_xref.csv --mapping=mapping/column_mapping.xlsx --out=output/tobe-sql --result=output/tobe-sql.xlsx --logEvery=100 --slowMs=500"
