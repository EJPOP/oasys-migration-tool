SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    CVLCPT_TASK_SECD /* 민원업무구분코드 */,
    FML_CLU_CD /* 수식항목코드 */,
    FML_CLU_NM || '(' || FML_CLU_CD || ')' AS FML_CLU_NM /* 수식항목명 */
FROM TBCSPAOE014D WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND CVLCPT_TASK_SECD = #CVLCPT_TASK_SECD# AND USE_YN = 'Y' ORDER BY FML_CLU_CD ASC