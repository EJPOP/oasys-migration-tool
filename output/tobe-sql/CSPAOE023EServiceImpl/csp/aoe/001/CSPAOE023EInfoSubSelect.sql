SELECT D1.CVLCPT_ADT_YR /* 민원감사년도 */,
    D1.TSK_NO /* 감사번호 */,
    D1.ADT_STP_SECD /* 감사단계구분코드 */,
    D1.PTCPT_CNB /* 참여자 전산번호 */,
    F_USR_INFO(D1.PTCPT_CNB, '2') AS INFO_RLPR_NM /* 정보관계자명 */,
    D1.BLT_DPT_CD /* 소속부서코드 */,
    D1.JBGD_SECD /* 직급구분코드 */,
    D1.JON_DVSN_CD /* 참여구분코드 */,
    D1.CTB_RT /* 기여비율 */,
    D1.EVL_RFR_SBJ_TXT /* 평가참고사항내용 */
FROM TBCSPAOE023M D1 WHERE 1=1 AND D1.CVLCPT_ADT_YR = #CVLCPT_ADT_YR# AND D1.TSK_NO = #strTskNo# AND D1.ADT_STP_SECD = #ADT_STP_SECD#