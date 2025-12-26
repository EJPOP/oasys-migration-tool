SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    CVLCPT_TASK_SECD /* 민원업무구분코드 */,
    ADT_KDCD /* 감사종류코드 */,
    UNW_RTE /* 불문율 */,
    AV_EVL_RK_VAL /* 평균평가등급값 */
FROM TBCSPAOE042M WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND CVLCPT_TASK_SECD = #CVLCPT_TASK_SECD# ORDER BY ADT_KDCD ASC