SELECT EVL_YR /* 평가년도 */,
    HLYR_DVSN_CD /* 반기구분코드 */,
    EVL_GP_CD /* 평가군코드 */,
    EVLT_CNB /* 평가자 전산자번호 */,
    BLT_DPT_CD /* 소속부서코드(평가) */,
    F_DPT_INFO(BLT_DPT_CD, '1') AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    JBGD_SECD /* 직급구분코드 */,
    F_CODE_NM('1000173', JBGD_SECD) AS JBGD_NM /* 직급명 */,
    F_USR_INFO(EVLT_CNB, '2') AS INFO_RLPR_NM /* 정보관계자명 */
FROM TBCSPAOE019D WHERE 1=1 AND EVL_YR = #strEvlYr# AND HLYR_DVSN_CD = #strHlyrDvsnCd# AND EVL_GP_CD = #strEvlGpCd# ORDER BY BLT_DPT_CD