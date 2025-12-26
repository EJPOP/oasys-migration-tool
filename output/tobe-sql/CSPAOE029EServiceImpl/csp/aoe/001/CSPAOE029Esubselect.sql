SELECT D1.EVL_YR /* 평가년도 */,
    D1.HLYR_DVSN_CD /* 반기구분코드 */,
    D1.TASK_KDCD /* 업무종류코드 */,
    D1.CHR_DPT_CD /* 담당부서코드 */,
    F_DPT_INFO(D1.CHR_DPT_CD, '1') AS TKCG_DEPT_NM /* 담당부서명 */,
    D1.TSK_NO /* 업무번호 */,
    D1.PTCPT_CNB /* 참여자 전산번호 */,
    D1.JBGD_SECD /* 직급구분코드 */,
    F_CODE_NM('1000173', D1.JBGD_SECD) AS JBGD_NM /* 직급명 */,
    D1.CTB_RT /* 기여비율 */,
    D1.EVL_RFR_SBJ_TXT /* 평가참고사항 */,
    F_USR_INFO(D1.PTCPT_CNB, '2') AS INFO_RLPR_NM /* 정보관계자명 */
FROM TBCSPAOE025M D1 WHERE 1=1 AND D1.EVL_YR = #strEvlYr# AND D1.HLYR_DVSN_CD = #strHlyrDvsnCd# AND D1.TASK_KDCD = #TASK_KDCD# AND D1.TSK_NO = #strTskNo# ORDER BY D1.CHR_DPT_CD, D1.PTCPT_CNB