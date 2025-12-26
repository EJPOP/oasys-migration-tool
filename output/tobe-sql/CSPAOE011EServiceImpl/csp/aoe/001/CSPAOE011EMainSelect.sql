SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    CVLCPT_TASK_SECD /* 민원업무구분코드 */,
    FML_TXT /* 수식내용 */
FROM TBCSPAOE013M WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND CVLCPT_TASK_SECD = #CVLCPT_TASK_SECD#