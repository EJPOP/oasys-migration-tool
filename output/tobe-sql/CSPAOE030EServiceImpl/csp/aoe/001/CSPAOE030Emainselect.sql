SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    CHR_DPT_CD /* 담당부서코드 */,
    F_DPT_INFO(CHR_DPT_CD, '1') AS TKCG_DEPT_NM /* 담당부서명 */,
    TSK_NO /* 업무번호 */,
    WGT_RTE /* 비중 */,
    TSK_NM /* 업무명 */,
    TSK_TXT /* 업무내용 */
FROM TBCSPAOE026M WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND CHR_DPT_CD = #strDeptCd# ORDER BY TSK_NO