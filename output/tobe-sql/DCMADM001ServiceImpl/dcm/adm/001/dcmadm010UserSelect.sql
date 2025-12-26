SELECT TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    USER_ID /* 사용자아이디 */,
    USER_NM /* 사용자명 */,
    JBGD_SECD /* 직급구분코드 */,
    SRECS_DEPT_CD /* 심사재심의부서코드 */,
    (SELECT AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */
FROM TBDCMACM002M D WHERE D.SRECS_DEPT_CD = U.SRECS_DEPT_CD AND ROWNUM USER_NM LIKE '%' || #emp_name# || '%' ORDER BY EMP_NAME