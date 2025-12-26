SELECT D1.EVL_YR AS EVL_YR /* 평가년도 */,
    D1.HLYR_DVSN_CD AS HLYR_DVSN_CD /* 반기구분코드 */,
    D1.CLU_CD AS CLU_CD /* 항목코드 */,
    D1.CLU_NM AS CLU_NM /* 항목명 */,
    D1.CLU_SC AS CLU_SC /* 항목점수 */,
    D1.CLU_TXT AS CLU_TXT /* 항목내용 */,
    NVL(D1.USE_YN, 'N') AS USE_YN /* 사용여부 */
FROM TBCSPAOE011M D1 WHERE 1=1 AND D1.EVL_YR = #strEvlYt# AND D1.HLYR_DVSN_CD = #strHlytDvsnCd# AND D1.USE_YN = #USE_YN# ORDER BY D1.CLU_CD