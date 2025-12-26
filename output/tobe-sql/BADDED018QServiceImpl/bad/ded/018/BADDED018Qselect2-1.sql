SELECT TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    /*전산번호*/ USER_NM AS USER_NM /* 사용자명 */,
    /*유저명*/ SRECS_DEPT_CD AS SRECS_DEPT_CD /* 심사재심의부서코드 */,
    /*부서코드*/ F_DPT_INFO(USR.SRECS_DEPT_CD,'1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    /*부서명*/ F_CODE_NM('1000173',USR.JBGD_SECD ) AS F_CODE_NM /* 직급명 */
FROM TBDCMACM001M USR WHERE 1=1 AND TRGT_NOP_ENO = #TRGT_NOP_ENO#