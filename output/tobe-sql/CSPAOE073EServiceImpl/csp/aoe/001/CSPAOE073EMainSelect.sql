SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    EVL_STR_DT /* 평가시작일 */,
    EVL_END_DT /* 평가종료일 */,
    ACMP_EVL_ASC_RT /* 실적평가 평점비율 */,
    MNSD_EVL_ASC_RT /* 다면평가 평점비율 */
FROM TBCSPAOE037M WHERE EVL_YR = #strEvlYr# ORDER BY EVL_YR, HLYR_DVSN_CD ASC