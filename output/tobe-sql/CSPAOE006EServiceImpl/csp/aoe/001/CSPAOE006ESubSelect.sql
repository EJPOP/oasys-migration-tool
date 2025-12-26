SELECT EVL_YR AS EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD AS HLYR_DVSN_CD /* 반기구분코드 */,
    CVLCPT_TASK_SECD AS CVLCPT_TASK_SECD /* 민원업무구분코드 */,
    AUD_TSK_KND_CD AS AUD_TSK_KND_CD /* 감사업무종류코드 */,
    RK_CD AS RK_CD /* 등급코드 */,
    RK_CD /* 등급코드 */,
    RK_ASC AS RK_ASC /* 등급평점 */,
    IDVT_INDEX_VAL AS IDVT_INDEX_VAL /* 성과가중치 개별지표 */,
    SUM_INDEX_VAL AS SUM_INDEX_VAL /* 성과가중치 합계지표 */,
    EVL_STAD_TXT AS EVL_STAD_TXT /* 평가기준내용 */,
    IDCT_YN AS IDCT_YN /* 표시여부 */
FROM TBCSPAOE010D WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND CVLCPT_TASK_SECD = #CVLCPT_TASK_SECD# AND AUD_TSK_KND_CD = #strAudTskKndCd# ORDER BY RK_CD