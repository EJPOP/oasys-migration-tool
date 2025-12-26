SELECT F_DPT_INFO(substr(A.SRECS_DEPT_CD,1,3)||'000','1') AS F_DPT_INFO /* 소속1 */,
    B.CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    F_CODE_NM('1000173',A.JBGD_SECD) AS JBGD_NM /* 직급명 */,
    A.USER_NM /* 사용자명 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    A.CAR_NO /* 차량번호 */,
    REGEXP_REPLACE(A.CNSTN_MBCMT_HOME_TELNO,'([[:digit:]]{3})([[:digit:]]{3,4})([[:digit:]]{4})','\1-\2-\3') AS CNSTN_MBCMT_HOME_TELNO /* 자문위원자택전화번호 */,
    A.CRU_TELNO /* 부패전화번호 */,
    A.CAR_NO_CHG_DT
FROM TBDCMACM001M A ,TBDCMACM002M B WHERE A.SRECS_DEPT_CD = B.SRECS_DEPT_CD AND A.SRECS_DEPT_CD != '880880' AND A.RQS_STA_CD = '3' AND substr(A.USER_ID,1,4) != 'test' AND A.USER_NM LIKE '%'||#str_USR_NAM#||'%' AND A.CAR_NO LIKE '%'||#str_CAR_NO#||'%' AND A.CAR_NO_CHG_DT >= #str_CAR_NO_CHG_DTS# AND A.CAR_NO_CHG_DT ORDER BY A.SRECS_DEPT_CD ASC