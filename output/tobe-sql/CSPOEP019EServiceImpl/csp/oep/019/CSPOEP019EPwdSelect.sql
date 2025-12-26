/* csp.oep.service.impl.CSPOEP019EServiceImpl.CSPOEP019EPwdSelect */ SELECT A.PWD_DVSN_CD /* 비밀번호구분코드 */,
    DGUARD.DECRYPT('TB_ENC','EAN',A.ESN) AS DECRYPT /* 비밀번호 */,
    DGUARD.DECRYPT('TB_ENC','EAN',A.BK_ESN) AS DECRYPT /* 이전비밀번호 */,
    A.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    A.INFO_RLPR_NM /* 정보관계자명 */,
    A.SRECS_DEPT_CD /* 심사재심의부서코드 */,
    A.FAIL_CNT /* 비밀번호 실패횟수 */,
    A.LOCK_YN /* 비밀번호 잠금여부 */
FROM TBCSPOEP007M A WHERE A.PWD_DVSN_CD = #strPwdDvsn#