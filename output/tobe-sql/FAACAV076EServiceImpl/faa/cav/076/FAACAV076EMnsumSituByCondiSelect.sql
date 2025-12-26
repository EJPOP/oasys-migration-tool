/* faa/cav/076/FAACAV076EMnsumSituByCondiSelect */ SELECT SUBSTR(YY.MNSUM_YM,1,4) || '/' || SUBSTR(YY.MNSUM_YM,5) AS MNSUM_YM /* 월계연월 */,
    YY.MNSUM_SECD /* 월계구분코드 */,
    F_CODE_NM( '1000711', YY.MNSUM_SECD ) AS F_CODE_NM,
    TO_CHAR(YY.CRU_RCPT_DT, 'YYYY-MM-DD HH24:MI:SS') AS CRU_RCPT_DT /* 부패접수일시 */
FROM ( SELECT ROWNUM,
    D2.MNSUM_YM /* 월계연월 */,
    D2.MNSUM_SECD /* 월계구분코드 */,
    D2.CRU_RCPT_DT /* 부패접수일시 */
FROM ( SELECT D1.MNSUM_YM /* 월계연월 */,
    D1.MNSUM_SECD /* 월계구분코드 */,
    D1.CRU_RCPT_DT /* 부패접수일시 */
FROM TWFAACAV044H D1 D1.MNSUM_YM = #MNSUM_YM# || #profMm# D1.MNSUM_YM LIKE #MNSUM_YM# || '%' D1.MNSUM_SECD = #MNSUM_SECD# ORDER BY MNSUM_YM, MNSUM_SECD, CRU_RCPT_DT ) D2 ) YY WHERE YY.RN BETWEEN ( #pageNo# - 1 ) * #pageSize# + 1 AND ( #pageNo# * #pageSize# )