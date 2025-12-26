SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    OFOD_DT /* 발령일 */,
    A.APM_DVSN_NM /* 임용구분명 */,
    A.JBPS_NM /* 직위명 */,
    A.JBGD_NM /* 직급명 */,
    A.APM_DPT_NM /* 임용부서명 */,
    A.OFOD_OFI_NM /* 발령청명 */
FROM TBCSPBII341L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC