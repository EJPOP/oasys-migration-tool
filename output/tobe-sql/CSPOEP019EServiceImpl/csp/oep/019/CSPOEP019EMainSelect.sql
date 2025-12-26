/* csp.oep.service.impl.CSPOEP019EServiceImpl.CSPOEP019EMainSelect */ SELECT *
FROM ( SELECT A.GRNDS_EXMN_AEX_SQNO /* 현장조사수용비순번 */,
    A.PWD_DVSN_CD /* 비밀번호구분코드 */,
    F_CODE_NM('1000999',A.PWD_DVSN_CD) AS F_CODE_NM /* 비밀번호구분명 */,
    DGUARD.DECRYPT('TB_ENC','EAN',A.ESN) AS DECRYPT /* 비밀번호 */,
    DGUARD.DECRYPT('TB_ENC','EAN',A.BK_ESN) AS DECRYPT /* 이전비밀번호 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    A.INFO_RLPR_NM /* 정보관계자명 */,
    A.SRECS_DEPT_CD /* 심사재심의부서코드 */,
    F_DPT_INFO(A.SRECS_DEPT_CD, '1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    TO_CHAR(A.LAST_MDFCN_DT,'yyyy-MM-dd') AS TO_CHAR
FROM TBCSPOEP007H A WHERE 1=1 AND TO_CHAR(A.LAST_MDFCN_DT,'yyyyMMdd') &gt;= #strChgDts# AND TO_CHAR(A.LAST_MDFCN_DT,'yyyyMMdd') &lt;= #strChgDte# AND A.PWD_DVSN_CD = #strPwdDvsn# ORDER BY A.GRNDS_EXMN_AEX_SQNO DESC ) AA WHERE rownum <= 30