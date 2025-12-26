/* csp.oep.service.impl.CSPOEP020EServiceImpl.CSPOEP020EMainSelect */ SELECT *
FROM ( SELECT ROWNUM,
    MSG_DVSN_CD /* 메시지 구분코드 */,
    GRNDS_EXMN_AEX_SQNO /* 현장조사수용비순번 */,
    CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */,
    JBPS_NM /* 직위명 */,
    JBGD_NM /* 직급명 */,
    INFO_RLPR_NM /* 정보관계자명 */,
    CNSTN_MBCMT_HOME_TELNO /* 자문위원자택전화번호 */,
    CRU_TELNO /* 부패전화번호 */,
    RMRK_CN /* 비고내용 */
FROM TBCSPOEP008M WHERE 1=1 AND MSG_DVSN_CD = #strMsgDvsnCd# AND INFO_RLPR_NM = #INFO_RLPR_NM# AND INFO_RLPR_NM LIKE '%'||#strNam#||'%' AND replace(CRU_TELNO,'-','') LIKE '%'||replace(#strTnb#,'-','')||'%' ORDER BY GRNDS_EXMN_AEX_SQNO ASC ) AA