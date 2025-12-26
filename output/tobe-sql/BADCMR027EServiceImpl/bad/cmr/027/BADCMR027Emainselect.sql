SELECT F_DPT_INFO(MNTR_PIC_OGDP_DEPT_CD, '1') AS F_DPT_INFO /* 담당자소속국과코드 */,
    F_DPT_INFO(SRECS_DEPT_CD, '1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    USER_NM AS USER_NM /* 사용자명 */,
    MNTR_PIC_OGDP_DEPT_CD /* 모니터링담당자소속부서코드 */,
    PIC_ENO /* 담당자직원전산번호 */,
    '' AS CHANGE_CNB
FROM ( SELECT MNTR_PIC_OGDP_DEPT_CD /* 모니터링담당자소속부서코드 */,
    PIC_ENO /* 담당자직원전산번호 */,
    F_USR_INFO(PIC_ENO, '2') AS USER_NM /* 사용자명 */,
    F_USR_INFO(PIC_ENO, '5') AS SRECS_DEPT_CD /* 심사재심의부서코드 */
FROM TBBADCMR015M ) WHERE 1 = 1 AND MNTR_PIC_OGDP_DEPT_CD LIKE SUBSTR(#strDptCd#, 0, 2) || '%' AND MNTR_PIC_OGDP_DEPT_CD != SRECS_DEPT_CD ORDER BY MNTR_PIC_OGDP_DEPT_CD