SELECT SRECS_DEPT_CD AS SRECS_DEPT_CD /* 심사재심의부서코드 */,
    F_DPT_INFO(SRECS_DEPT_CD, '1') AS DEPT_NM /* 부서명 */,
    HIRK_DPT_CD
FROM TBDCMACM002M WHERE DEPT_TPCD = 'A' /*지원부서만 가져온다. USE_YN컬럼을 안건 이유는 그것을 걸 경우 폐지된 부서는 볼수 없기 때문이다*/ AND SUBSTR(SRECS_DEPT_CD, 3) != '0000'