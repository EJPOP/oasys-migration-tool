SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    SEQ_NO /* 순서번호 */,
    JON_NOP_MORE_CNT /* 참여인원이상수 */,
    JON_NOP_LES_CNT /* 참여인원이하수 */,
    MAXM_RT /* 상한비율 */
FROM TBCSPAOE015M WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# ORDER BY SEQ_NO ASC