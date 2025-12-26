SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.WRK_STR_DT /* 근무시작일 */,
    A.WRK_END_DT /* 근무종료일 */,
    A.SLCT_OOFC_NM /* 청탁근무처명 */,
    A.JBPS_NM /* 직위명 */,
    A.CHR_OW_NM /* 담당사무명 */
FROM TBCSPBII342L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC