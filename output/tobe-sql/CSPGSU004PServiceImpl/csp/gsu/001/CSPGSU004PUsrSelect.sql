SELECT USER_ID /* 사용자아이디 */,
    USER_NM /* 사용자명 */,
    SRECS_DEPT_CD /* 심사재심의부서코드 */,
    F_DPT_INFO(SRECS_DEPT_CD,'1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    100 AS CTB_RT,
    '' AS PRPS_YE,
    '' AS PRPS_SLN
FROM TBDCMACM001M WHERE 1=1 AND USER_NM LIKE '%'||#strUSR_NAM#||'%' AND TRGT_NOP_ENO = #TRGT_NOP_ENO#