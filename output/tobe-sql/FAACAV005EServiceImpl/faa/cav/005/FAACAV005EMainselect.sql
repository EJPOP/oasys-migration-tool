/* faa/cav/005/FAACAV005EMainselect */ SELECT ROWNUM,
    MNSUM_YM /* 월계연월 */,
    DECODE(MNSUM_SECD,'01','세입금','02','세출금','03','특별계정','05','국고예금') AS MNSUM_SECD /* 월계구분코드 */,
    CRU_RCPT_DT /* 부패접수일시 */
FROM TWFAACAV044H WHERE 1 = 1 AND MNSUM_YM = #MNSUM_YM# AND MNSUM_YM BETWEEN #strYr#||'01' AND #strYr#||'13' AND MNSUM_YM LIKE '____'||#strMm# AND MNSUM_SECD LIKE #strMnDvsn# ORDER BY KEY, MNSUM_YM, MNSUM_SECD, CRU_RCPT_DT