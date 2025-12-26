SELECT D1.EVL_YR /* 평가년도 */,
    D1.HLYR_DVSN_CD /* 반기구분코드 */,
    D1.EVL_DPT_CD /* 평가부서코드 */,
    D1.TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    D1.EVL_DPT_NM /* 평가부서명 */,
    D1.JBPS_CD /* 직위코드 */,
    F_CODE_NM('1000176', D1.JBPS_CD) AS JBPS_NM /* 직위명 */,
    D1.INFO_RLPR_NM /* 정보관계자명 */,
    D1.JBGD_SECD /* 직급구분코드 */,
    F_CODE_NM('1000173', D1.JBGD_SECD) AS JBGD_NM /* 직급명 */,
    D1.BLT_DPT_CD /* 소속부서코드 */,
    D1.CRU_OGDP_DEPT_NM /* 부패소속부서명 */,
    D1.HRA_MV_DT /* 인사이동일자 */
FROM TBCSPAOE041M D1 /*[성과평가]인사이력관리 테이블*/ WHERE 1 = 1 AND D1.EVL_YR = #strEvlYr# AND D1.HLYR_DVSN_CD = #strHlyrDvsnCd# AND D1.EVL_DPT_CD = #strEvlDptCd# AND ( D1.INFO_RLPR_NM LIKE '%' || #strName# || '%' OR D1.TRGT_NOP_ENO LIKE '%' || #strName# || '%' ) ORDER BY D1.EVL_DPT_CD, TRGT_NOP_ENO