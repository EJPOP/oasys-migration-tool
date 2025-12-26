SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    PTCPT_CNB /* 참여자 전산번호 */,
    TSK_NO /* 업무번호 */,
    BLT_DPT_CD /* 소속부서코드 */,
    JBGD_SECD /* 직급구분코드 */,
    TSK_NM /* 업무명 */,
    TSK_TXT /* 업무내용 */
FROM TBCSPAOE028M WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND PTCPT_CNB = #strPtcptCnb# ORDER BY TSK_NO