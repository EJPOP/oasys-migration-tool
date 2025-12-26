SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.ACQ_DT /* 취득일 */,
    A.QLF_NM /* 자격명 */,
    A.ENF_ORG_NM /* 시행기관명 */
FROM TBCSPBII330L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC