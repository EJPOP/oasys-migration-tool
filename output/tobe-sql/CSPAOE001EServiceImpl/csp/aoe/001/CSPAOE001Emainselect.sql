SELECT EVL_YR AS EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD AS HLYR_DVSN_CD /* 반기구분코드 */,
    EVL_STR_DT AS EVL_STR_DT /* 평가시작일 */,
    EVL_END_DT AS EVL_END_DT /* 평가종료일 */,
    SRCH_STR_DT /* 조회시작일 */,
    SRCH_END_DT /* 조회종료일 */
FROM TBCSPAOE001M WHERE 1=1 AND EVL_YR = #strEvlYr# ORDER BY EVL_YR DESC, HLYR_DVSN_CD ASC