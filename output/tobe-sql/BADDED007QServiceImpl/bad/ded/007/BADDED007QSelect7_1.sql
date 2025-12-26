select A.USER_NM /* 사용자명 */,
    A.SRECS_DEPT_CD /* 심사재심의부서코드 */,
    A.JBGD_SECD /* 직급구분코드 */,
    F_CODE_NM('1000173',JBGD_SECD) AS JBGD_NM /* 직급명 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    B.CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */
from TBDCMACM001M A , TBDCMACM002M B where 1=1 and A.TRGT_NOP_ENO LIKE #strBztTlCnb# /*인자값 4*/ and B.SRECS_DEPT_CD = A.SRECS_DEPT_CD