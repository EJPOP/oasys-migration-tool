SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.EXM_DT /* 시험일 */,
    A.EXM_NM /* 시험명 */,
    A.APM_APT_RANK_NM /* 임용예정직급명 */,
    A.ENF_ORG_NM /* 시행기관명 */
FROM TBCSPBII340L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC